import type { TrialBalanceResponse } from '../types/ledger';

import './TrialBalanceTable.css';

interface Props {
  trialBalanceData: TrialBalanceResponse;
}

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

export function TrialBalanceTable({ trialBalanceData }: Props) {
  return (
    <div>
      <h2>Trial Balance</h2>
      <p>
        as of{' '}
        {new Date(trialBalanceData.asOf).toLocaleString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          hour: 'numeric',
          minute: '2-digit',
          timeZoneName: 'short',
        })}
      </p>

      <table>
        <thead>
          <tr>
            <th>Code</th>
            <th>Account Name</th>
            <th>Debit</th>
            <th>Credit</th>
          </tr>
        </thead>
        <tbody>
          {trialBalanceData.entries.map((entry) => (
            <tr key={entry.accountCode}>
              <td>{entry.accountCode}</td>
              <td>{entry.accountName}</td>
              <td className='money-cell'>
                {currencyFormatter.format(entry.totalDebit)}
              </td>
              <td className='money-cell'>
                {currencyFormatter.format(entry.totalCredit)}
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td className='totals-cell' colSpan={2}>
              Totals
            </td>
            <td className='money-cell totals-cell'>
              {currencyFormatter.format(trialBalanceData.totalDebits)}
            </td>
            <td className='money-cell totals-cell'>
              {currencyFormatter.format(trialBalanceData.totalCredits)}
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
