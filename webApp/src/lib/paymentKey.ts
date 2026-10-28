/*
 * The names of the shared payment key enums, as the bridge sends them. Typing them lets each one pick
 * its message key (`paymentKey_type${type}`) with the type check making sure every case has words.
 */
export type PaymentMethod = 'PIX' | 'BITCOIN'
export type PaymentKeyType = 'CPF' | 'PHONE' | 'EMAIL' | 'RANDOM' | 'LIGHTNING' | 'ONCHAIN'
export type BitcoinKeyType = 'LIGHTNING' | 'ONCHAIN'
export type PaymentKeyFormat = 'CPF' | 'PHONE' | 'EMAIL' | 'RANDOM' | 'LIGHTNING_ADDRESS' | 'LNURL' | 'SEGWIT' | 'TAPROOT'
export type PaymentKeyError =
  | 'INVALID_CPF'
  | 'INVALID_PHONE'
  | 'INVALID_EMAIL'
  | 'INVALID_RANDOM'
  | 'INVALID_LIGHTNING'
  | 'LIGHTNING_INVOICE'
  | 'INVALID_ONCHAIN'
  | 'TESTNET_ADDRESS'
  | 'ALREADY_SAVED'
