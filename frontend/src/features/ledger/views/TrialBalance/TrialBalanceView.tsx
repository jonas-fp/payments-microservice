import { useTrialBalance } from '../../hooks/useTrialBalance';
import { TrialBalanceTable } from '../../components/TrialBalanceTable';

export function TrialBalanceView() {
  const { trialBalanceData, isLoading, error } = useTrialBalance();

  if (isLoading) return <div>Loading trial balance...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!trialBalanceData) return <div>No trial balance data available...</div>;

  return (
    <div>
      <TrialBalanceTable trialBalanceData={trialBalanceData} />
    </div>
  );
}
