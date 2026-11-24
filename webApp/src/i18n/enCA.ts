import type { MessageKey } from './en'

/**
 * What Canada writes differently from the American English of `en.ts` ("colour", "grey", "centre", but "math" and
 * "recognized" as the United States). `index.ts` merges it over `en` for Canada, and under `enGB.ts` for
 * Britain and Australia, so a key missing here simply keeps the American wording.
 */
export const enCA: Partial<Record<MessageKey, string>> = {
  profileSetup_avatarColor: 'Avatar colour',
  editGroup_iconColor: 'Icon colour',
  deleteAccount_deletedProfile: 'Your profile (name, photo and colour)',
  editProfile_avatarColor: 'Avatar colour',
  payingMethod_color: 'Colour',
  activity_fieldColor: 'Colour',
  splitBill_categoryColor: 'Colour',
  categoryColor_gray: 'Grey',
  categoryMap_changeColor: 'Change colour of {name}',
  categoryMap_restoreColor: 'Restore colour',
  customColor_add: 'Add colour',
  customColor_title: 'New colour',
  customColor_label: 'Custom colour {hex}',
  customColor_full: 'Your palette is full: long press one of your colours to delete it.',
  customColor_hint: 'Long press one of your colours to delete it.',
  customColor_rightFull: 'Your palette is full: right-click one of your colours to delete it.',
  customColor_rightHint: 'Right-click one of your colours to delete it.',
  customColor_deleteTitle: 'Delete this colour?',
  customColor_inUseTitle: 'This colour is in use',
  customColor_inUseMessage: '{name} uses this colour. Change its colour first, then delete it.',
  createGroup_changeColor: "Change the group's colour",
  members_releaseMessage: "Whoever joined as {name} leaves the group. {name}'s name, colour and bills stay, and {name} can be picked again through the group's link.",
}
