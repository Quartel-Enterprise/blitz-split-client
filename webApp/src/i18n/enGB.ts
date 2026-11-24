import type { MessageKey } from './en'

/**
 * What Britain writes differently from Canada (`enCA.ts`), whose spelling it shares ("colour", "grey", "centre"): the
 * few words Canada writes as the United States ("maths", "recognised", "towards"). `index.ts` merges `enCA` and then
 * this over `en` for Britain, for Australia, which writes the same, and for any other English region but the American
 * ones (Pierre's call).
 */
export const enGB: Partial<Record<MessageKey, string>> = {
  billDetail_noteOwesPayer: 'Counts towards what you owe {name} in Balances.',
  paymentKey_validRANDOM: 'Random key recognised (32 characters)',
  globalBalances_message: 'Hi {name}! By the Blitz Split maths, you owe me {amount} from {groups}.',
  simplifyChange_hint: 'If you already sent the money, mark it as paid anyway: the app redoes the maths.',
}
