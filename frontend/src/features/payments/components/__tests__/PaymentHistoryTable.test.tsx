import { render, screen } from '@testing-library/react';
import { PaymentHistoryTable } from '../PaymentHistoryTable';
import type { PaymentEvent } from '../../types/payments';

const mockHistory: PaymentEvent[] = [
  {
    id: 'evt-1',
    eventType: 'AUTHORIZE_SUCCESS',
    processorEventReference: 'proc-ref-1',
    createdAt: '2026-08-31T20:00:00Z',
  },
  {
    id: 'evt-2',
    eventType: 'CAPTURE_SUCCESS',
    processorEventReference: 'proc-ref-2',
    createdAt: '2026-08-31T21:00:00Z',
  },
];

describe('PaymentHistoryTable', () => {
  it('renders a row for each event with its type and processor reference', () => {
    render(<PaymentHistoryTable history={mockHistory} />);

    expect(screen.getByText('AUTHORIZE_SUCCESS')).toBeInTheDocument();
    expect(screen.getByText('CAPTURE_SUCCESS')).toBeInTheDocument();
    expect(screen.getByText('proc-ref-1')).toBeInTheDocument();
    expect(screen.getByText('proc-ref-2')).toBeInTheDocument();
  });

  it('renders only the header row when history is empty', () => {
    render(<PaymentHistoryTable history={[]} />);

    expect(screen.getAllByRole('row')).toHaveLength(1);
  });
});
