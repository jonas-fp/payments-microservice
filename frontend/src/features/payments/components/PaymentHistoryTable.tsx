import type { PaymentDetailsResponse } from '../types/payments';

import './PaymentHistoryTable.css';

interface Props {
  history: PaymentDetailsResponse['history'];
}

export function PaymentHistoryTable({ history }: Props) {
  return (
    <table className='payment-history-table'>
      <thead>
        <tr>
          <th>Event</th>
          <th>Processor Event Reference</th>
          <th>Date</th>
        </tr>
      </thead>
      <tbody>
        {history.map((event) => (
          <tr key={event.id}>
            <td>{event.eventType}</td>
            <td>{event.processorEventReference}</td>
            <td>{new Date(event.createdAt).toLocaleString()}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
