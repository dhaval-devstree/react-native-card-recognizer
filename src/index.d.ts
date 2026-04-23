export type PermissionPrompt = {
  title?: string;
  description?: string;
};

export type ScanCardOptions = Record<string, unknown>;

export type ScanPaymentCardParams = {
  permission?: PermissionPrompt;
  scannerText?: ScanCardOptions;
};

export type ScanPaymentCardResult = {
  cardNumber: string;
  cardHolderName: string;
  expirationDate: string;
};

export declare function scanPaymentCard(
  params?: ScanPaymentCardParams
): Promise<ScanPaymentCardResult>;

