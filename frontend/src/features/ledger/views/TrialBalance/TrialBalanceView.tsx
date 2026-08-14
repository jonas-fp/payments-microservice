import { useEffect, useState } from 'react';
import type { TrialBalanceResponse } from '../../types/ledger';

export function TrialBalanceView() {
  const [trialBalance, setTrialBalance] = useState<TrialBalanceResponse | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('/api/v1/payments/subledger/trial-balance')
      .then((res) => {
        if (!res.ok) throw new Error('Fetch response not in 2xx range');
        return res.json();
      })
      .then((data) => setTrialBalance(data))
      .catch((err) => {
        console.error('Error fetching trial balance: ', err);
        setError('Error fetching trial balance');
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
