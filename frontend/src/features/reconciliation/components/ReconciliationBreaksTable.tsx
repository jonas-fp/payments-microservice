import type {
  ReconciliationBreakType,
  ReconciliationRunSummary,
} from '../types/reconciliation';

import './ReconciliationBreaksTable.css';

interface Props {
  runData: ReconciliationRunSummary;
}

const breakLabels: Record<ReconciliationBreakType, string> = {
  AMOUNT_MISMATCH: 'Amount mismatch',
  MISSING_INTERNAL_RECORD: 'Missing internal record',
  MISSING_PROCESSOR_RECORD: 'Missing processor record',
  DUPLICATE_INTERNAL_RECORD: 'Duplicate internal record',
  DUPLICATE_PROCESSOR_RECORD: 'Duplicate processor record',
};

export function ReconciliationBreaksTable({ runData }: Props) {
  const breaks = Object.entries(runData.breakSummary) as [
    ReconciliationBreakType,
    number,
  ][];
  const total = breaks.reduce((sum, [, count]) => sum + count, 0);

  if (breaks.length === 0) {
    return <p>No reconciliation breaks were found for this run.</p>;
  }

  return (
    <table className='reconciliation-breaks-table'>
      <thead>
        <tr>
          <th>Break Type</th>
          <th>Count</th>
        </tr>
      </thead>
      <tbody>
        {breaks.map(([breakType, count]) => (
          <tr key={breakType}>
            <td>{breakLabels[breakType]}</td>
            <td className='count-cell'>{count}</td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <td className='totals-cell'>Total</td>
          <td className='count-cell totals-cell'>{total}</td>
        </tr>
      </tfoot>
    </table>
  );
}
