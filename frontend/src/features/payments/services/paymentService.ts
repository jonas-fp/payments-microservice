import { axiosInstance } from '../../../services/axiosInstance';
import type { PaymentDetailsResponse } from '../types/payments';

export const paymentService = {
  getPaymentDetails: async (
    paymentId: string,
  ): Promise<PaymentDetailsResponse> => {
    const response = await axiosInstance.get<PaymentDetailsResponse>(
      `/v1/payments/${paymentId}`,
    );
    return response.data;
  },
};
