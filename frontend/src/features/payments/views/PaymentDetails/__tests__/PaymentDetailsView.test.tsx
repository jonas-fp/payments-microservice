import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { PaymentDetailsView } from '../PaymentDetailsView';

const paymentId = '123e4567-e89b-12d3-a456-426614174000';

describe('PaymentDetailsView Error States', () => {
  it('displays an error message when the API call fails', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <MemoryRouter initialEntries={[`/payments/payment-details/${paymentId}`]}>
        <Routes>
          <Route
            path='/payments/payment-details/:paymentId'
            element={<PaymentDetailsView />}
          />
        </Routes>
      </MemoryRouter>,
    );

    const errorElement = await screen.findByText(
      /Something went wrong while loading this payment. Please try again./i,
    );
    expect(errorElement).toBeInTheDocument();
    expect(errorElement).toHaveClass('payment-details-error');

    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});
