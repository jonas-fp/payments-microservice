import { useEffect, useState } from 'react';
import type { TrialBalanceResponse } from '../../types/ledger';
import { ledgerService } from '../../services/ledgerService';

export function TrialBalanceView() {
  const [trialBalance, setTrialBalance] = useState<TrialBalanceResponse | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    ledgerService
      .getTrialBalance()
      .then((res) => {
        setTrialBalance(res);
      })
      .catch((err) => {
        console.error('Error fetching trial balance: ', err);
        setError(`'Failed to load trial balance data: ${err.message}`);
      });
  }, []);

  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!trialBalance) return <div>Loading trial balance...</div>;

  return (
    <div>
      <p>Here is the trial-balance data from the backend:</p>
      <pre
        style={{ background: '#f4f4f4', padding: '10px', borderRadius: '5px' }}
      >
        {JSON.stringify(trialBalance, null, 2)}
      </pre>
    </div>
  );
}
