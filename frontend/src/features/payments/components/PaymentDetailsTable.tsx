import type { PaymentDetailsResponse } from '../types/payments';

import './PaymentDetailsTable.css';

interface Props {
  paymentData: PaymentDetailsResponse;
}

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

const toMajor = (minorAmount: number) =>
  currencyFormatter.format(minorAmount / 100);

export function PaymentDetailsTable({ paymentData }: Props) {
  return (
    <table className='payment-details-table'>
      <tbody>
        <tr>
          <th>Payment ID</th>
          <td>{paymentData.id}</td>
        </tr>
        <tr>
          <th>Status</th>
          <td>{paymentData.status}</td>
        </tr>
        <tr>
          <th>Customer ID</th>
          <td>{paymentData.customerId}</td>
        </tr>
        <tr>
          <th>Invoice ID</th>
          <td>{paymentData.invoiceId}</td>
        </tr>
        <tr>
          <th>Authorized Amount</th>
          <td className='money-cell'>
            {toMajor(paymentData.authorizedAmount)}
          </td>
        </tr>
        <tr>
          <th>Captured Amount</th>
          <td className='money-cell'>{toMajor(paymentData.capturedAmount)}</td>
        </tr>
        <tr>
          <th>Refunded Amount</th>
          <td className='money-cell'>{toMajor(paymentData.refundedAmount)}</td>
        </tr>
        <tr>
          <th>Processor Reference</th>
          <td>{paymentData.processorReference}</td>
        </tr>
        <tr>
          <th>Created At</th>
          <td>{new Date(paymentData.createdAt).toLocaleString()}</td>
        </tr>
      </tbody>
    </table>
  );
}
