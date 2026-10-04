import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { server } from '../../../../../mocks/server';
import { ReconciliationBreaksView } from '../ReconciliationBreaksView';

const runId = '123e4567-e89b-12d3-a456-426614174000';

function renderView(initialEntry = '/reconciliation/breaks') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route
          path='/reconciliation/breaks'
          element={<ReconciliationBreaksView />}
        />
        <Route
          path='/reconciliation/breaks/:runId'
          element={<ReconciliationBreaksView />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ReconciliationBreaksView', () => {
  it('prompts for a run ID before making a request', () => {
    renderView();

    expect(
      screen.getByText(
        'Enter a reconciliation run UUID above to view its breaks.',
      ),
    ).toBeInTheDocument();
  });

  it('validates the run ID before navigating', async () => {
    const user = userEvent.setup();
    renderView();

    await user.type(
      screen.getByLabelText('Reconciliation run ID'),
      'not-a-uuid',
    );
    await user.click(screen.getByRole('button', { name: 'Look up' }));

    expect(
      screen.getByText(/expected a UUID/i),
    ).toBeInTheDocument();
  });

  it('loads a run after a valid lookup', async () => {
    server.use(
      http.get(
        '/api/v1/payments/reconciliation/runs/:runId',
        ({ params }) => {
          return HttpResponse.json({
            id: params.runId,
            businessDate: '2026-10-02',
            status: 'SUCCEEDED',
            startedAt: '2026-10-02T12:00:00Z',
            completedAt: '2026-10-02T12:00:01Z',
            breakSummary: { AMOUNT_MISMATCH: 1 },
          });
        },
      ),
    );
    const user = userEvent.setup();
    renderView();

    await user.type(screen.getByLabelText('Reconciliation run ID'), runId);
    await user.click(screen.getByRole('button', { name: 'Look up' }));

    expect(await screen.findByText('Amount mismatch')).toBeInTheDocument();
    expect(screen.getByText('Succeeded')).toBeInTheDocument();
  });

  it('shows a friendly error when loading fails', async () => {
    const consoleSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined);
    renderView(`/reconciliation/breaks/${runId}`);

    expect(
      await screen.findByText(
        'Something went wrong while loading this reconciliation run. Please try again.',
      ),
    ).toBeInTheDocument();
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});
