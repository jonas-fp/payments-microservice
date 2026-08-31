import { render, screen } from '@testing-library/react';
import { TrialBalanceView } from '../TrialBalanceView';
import { MemoryRouter } from 'react-router-dom';

describe('TrialBalanceView Error States', () => {
  it('displays an error message when the API call fails', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <MemoryRouter>
        <TrialBalanceView />
      </MemoryRouter>,
    );

    const errorElement = await screen.findByText(
      /Failed to load trial balance data: Request failed with status code 500/i,
    );
    expect(errorElement).toBeInTheDocument();
    expect(errorElement).toHaveStyle({ color: 'rgb(255, 0, 0)' });

    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});
