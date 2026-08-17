import type { TrialBalanceResponse } from '../types/ledger';

interface Props {
  trialBalanceData: TrialBalanceResponse;
}

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

export default function TrialBalanceTable({ trialBalanceData }: Props) {
  return (
    <div>
      <h2>
        Trial Balance (as of{' '}
        {new Date(trialBalanceData.asOf).toLocaleString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          hour: 'numeric',
          minute: '2-digit',
          timeZoneName: 'short',
        })}
        )
      </h2>

      <div>
        Status: {trialBalanceData.isBalanced ? 'balanced' : 'unbalanced'}
      </div>

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
              <td>{currencyFormatter.format(entry.totalDebit)}</td>
              <td>{currencyFormatter.format(entry.totalCredit)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td colSpan={2}>Totals</td>
            <td>{currencyFormatter.format(trialBalanceData.totalDebits)}</td>
            <td>{currencyFormatter.format(trialBalanceData.totalCredits)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
