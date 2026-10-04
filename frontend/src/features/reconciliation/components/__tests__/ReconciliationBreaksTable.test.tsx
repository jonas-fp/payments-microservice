import { render, screen } from '@testing-library/react';
import { ReconciliationBreaksTable } from '../ReconciliationBreaksTable';
import type { ReconciliationRunSummary } from '../../types/reconciliation';

const runData: ReconciliationRunSummary = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  businessDate: '2026-10-02',
  status: 'SUCCEEDED',
  startedAt: '2026-10-02T12:00:00Z',
  completedAt: '2026-10-02T12:00:01Z',
  breakSummary: {
    AMOUNT_MISMATCH: 1,
    MISSING_INTERNAL_RECORD: 2,
    MISSING_PROCESSOR_RECORD: 3,
  },
};

describe('ReconciliationBreaksTable', () => {
  it('renders human-readable break labels and counts', () => {
    render(<ReconciliationBreaksTable runData={runData} />);

    expect(screen.getByText('Amount mismatch')).toBeInTheDocument();
    expect(screen.getByText('Missing internal record')).toBeInTheDocument();
    expect(screen.getByText('Missing processor record')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
  });

  it('renders an empty state when the run has no breaks', () => {
    render(
      <ReconciliationBreaksTable
        runData={{ ...runData, breakSummary: {} }}
      />,
    );

    expect(
      screen.getByText('No reconciliation breaks were found for this run.'),
    ).toBeInTheDocument();
  });
});
