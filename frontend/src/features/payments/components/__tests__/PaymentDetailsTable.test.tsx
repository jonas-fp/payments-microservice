import { render, screen } from '@testing-library/react';
import { PaymentDetailsTable } from '../PaymentDetailsTable';
import type { PaymentDetailsResponse } from '../../types/payments';

const mockData: PaymentDetailsResponse = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  customerId: 'cust_123',
  invoiceId: '223e4567-e89b-12d3-a456-426614174000',
  authorizedAmount: 150000,
  capturedAmount: 150000,
  refundedAmount: 0,
  currency: 'USD',
  status: 'CAPTURED',
  processorReference: 'proc_ref_789',
  createdAt: '2026-08-31T23:59:59Z',
  history: [],
};

describe('PaymentDetailsTable', () => {
  it('renders minor-unit amounts converted to major units and formatted as USD', () => {
    render(<PaymentDetailsTable paymentData={mockData} />);

    const elements = screen.queryAllByText('$1,500.00');
    expect(elements.length).toEqual(2);
  });

  it('renders a zero amount as $0.00', () => {
    render(<PaymentDetailsTable paymentData={mockData} />);

    expect(screen.getByText('$0.00')).toBeInTheDocument();
  });

  it('renders the payment id and status', () => {
    render(<PaymentDetailsTable paymentData={mockData} />);

    expect(screen.getByText(mockData.id)).toBeInTheDocument();
    expect(screen.getByText(mockData.status)).toBeInTheDocument();
  });
});
