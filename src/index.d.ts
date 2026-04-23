export type PermissionPrompt = {
  title?: string;
  description?: string;
};

export type AndroidScanCardOptions = {
  hint?: string;
  toolbarTitle?: string;
  /**
   * Shows the "Add manually" button (and uses `cardFrameColor` for its text color).
   */
  manualInputButtonText?: string;
  /**
   * Android scanner primary UI color.
   *
   * Supported formats:
   * - Hex string, e.g. "#RRGGBB" or "#AARRGGBB"
   * - Android color resource name, e.g. "@color/primary_color_dark"
   * - ARGB int (e.g. from React Native `processColor(...)`)
   */
  cardFrameColor?: string | number;
  /**
   * @deprecated Use `cardFrameColor` instead.
   */
  mainColor?: string | number;
};

export type ScanCardOptions = {
  /**
   * Cross-platform card frame color shortcut.
   *
   * - Android: same as `android.cardFrameColor` (also accepts legacy `android.mainColor`)
   * - iOS: same as `ios.cardFrameColor` (also accepts legacy `ios.mainColor`)
   */
  cardFrameColor?: string | number;
  android?: AndroidScanCardOptions;
  ios?: {
    hint?: string;
    statusLookingForCardNumber?: string;
    statusReadingHoldSteady?: string;
    statusNumberFoundLookingForExpiry?: string;
    cancel?: string;
    done?: string;
    torch?: string;
    /**
     * iOS card frame color.
     * Supported formats: "#RRGGBB" or "#AARRGGBB".
     */
    cardFrameColor?: string;
    /**
     * @deprecated Use `cardFrameColor` instead.
     */
    mainColor?: string;
  } & Record<string, unknown>;
} & Record<string, unknown>;

export type ScanPaymentCardParams = {
  permission?: PermissionPrompt;
  scannerText?: ScanCardOptions;
  /**
   * Shorthand for `scannerText.cardFrameColor`.
   */
  cardFrameColor?: string | number;
};

export type ScanPaymentCardResult = {
  cardNumber: string;
  cardHolderName: string;
  expirationDate: string;
};

export declare function scanPaymentCard(
  params?: ScanPaymentCardParams
): Promise<ScanPaymentCardResult>;
