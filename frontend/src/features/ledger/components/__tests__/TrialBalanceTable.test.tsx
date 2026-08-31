import { render, screen } from '@testing-library/react';
import { TrialBalanceTable } from '../TrialBalanceTable';

const mockData = {
  asOf: '2026-08-31T23:59:59Z',
  totalDebits: 1000,
  totalCredits: 1000,
  isBalanced: true,
  entries: [
    {
      accountCode: '1000',
      accountName: 'Cash',
      totalDebit: 1000,
      totalCredit: 0,
    },
    {
      accountCode: '1001',
      accountName: 'Revenue',
      totalDebit: 0,
      totalCredit: 1000
    },
  ],
};

describe('TrialBalanceTable', () => {
  it('renders the right number of currency values formatted as USD', () => {
    render(<TrialBalanceTable trialBalanceData={mockData} />)

    const elements = screen.queryAllByText('$1,000.00');
    expect(elements.length).toEqual(4);
  })

  it('renders the right number of zero values as dashes',  () => {
    render(<TrialBalanceTable trialBalanceData={mockData} />)

    const elements = screen.queryAllByText('—');
    expect(elements.length).toEqual(2);
  })

  
});
