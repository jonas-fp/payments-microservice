import { useState } from 'react';
import { useTrialBalance } from '../../hooks/useTrialBalance';
import { TrialBalanceTable } from '../../components/TrialBalanceTable';
import { useSearchParams } from 'react-router-dom';
import { toAsOfParam } from '../../utils/asOf';
import { Loader } from '../../../../components/Loader';

import './TrialBalanceView.css';

export function TrialBalanceView() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedDate = searchParams.get('asOf') ?? '';
  const [refreshSignal, setRefreshSignal] = useState<boolean>(false);

  const { trialBalanceData, isLoading, error } = useTrialBalance(
    refreshSignal,
    toAsOfParam(selectedDate),
  );

  if (isLoading) return <Loader />;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!trialBalanceData) return <div>No trial balance data available...</div>;

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

      <div className='trial-balance-options'>
        <input
          type='date'
          value={selectedDate}
          onChange={(e) => {
            const date = e.target.value;
            setSearchParams(date ? { asOf: date } : {});
          }}
        />

        <button
          className='refresh-button'
          onClick={() => setRefreshSignal((prev) => !prev)}
        >
          Refresh
        </button>
      </div>

      <TrialBalanceTable trialBalanceData={trialBalanceData} />
    </div>
  );
}
