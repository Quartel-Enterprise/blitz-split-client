import Shared
import SwiftUI

/// The words of "Teach the map?" (9.3e, 9.3f), composed from the state.
enum CategoryTeachCopy {
    static func offer(_ teach: CategoryTeach) -> String {
        let saved = CategoryCopy.path(teach.category, parent: teach.categoryParent)
        guard let map = teach.mapCategory else { return String(localized: "Bill saved in \(saved). Teach the map?") }
        let suggested = CategoryCopy.path(map, parent: teach.mapCategoryParent)
        return String(localized: "Bill saved in \(saved), not in \(suggested), which the map suggested. Teach the map?")
    }

    static func question(_ teach: CategoryTeach) -> String {
        let target = CategoryCopy.path(teach.category, parent: teach.categoryParent)
        return String(localized: "Which word of “\(teach.title)” should pick \(target) on the next bills?")
    }

    static func detail(_ word: TeachWord, teach: CategoryTeach) -> String {
        let target = CategoryCopy.path(teach.category, parent: teach.categoryParent)
        guard let current = word.currentCategory else {
            return String(localized: "New in the map. Titles with “\(word.term)” go to \(target).")
        }
        return String(localized: "Leaves \(CategoryCopy.name(current)): titles with “\(word.term)” go to \(target).")
    }
}

/// "Bill saved in Leisure › Sports, not in Home › Rent, which the map suggested. Teach the map?" with Not now and
/// Teach, above the bottom of Expenses once a bill whose category was picked by hand is saved (9.3e).
struct CategoryTeachBanner: View {
    let teach: CategoryTeach
    let model: GroupDetailModel

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(verbatim: CategoryTeachCopy.offer(teach))
                .font(.subheadline)
                .fixedSize(horizontal: false, vertical: true)
            HStack {
                Spacer()
                Button("Not now") {
                    model.send(GroupDetailUiEventOnTeachDismissed.shared)
                }
                Button("Teach") {
                    model.send(GroupDetailUiEventOnTeachClicked.shared)
                }
                .buttonStyle(.glassProminent)
            }
        }
        .padding(16)
        .glassEffect(.regular, in: .rect(cornerRadius: 22))
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
        .accessibilityElement(children: .contain)
    }
}

/// "Teach the map" (9.3f): which word of the title picks the category on the next bills.
struct CategoryTeachSheet: View {
    let teach: CategoryTeach
    let model: GroupDetailModel

    var body: some View {
        Form {
            Section {
                ForEach(teach.words, id: \.key) { word in
                    let isSelected = word.key == teach.selectedKey
                    Button {
                        model.send(GroupDetailUiEventOnTeachWordPicked(key: word.key))
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                                .foregroundStyle(isSelected ? Palette.accent : .secondary)
                                .accessibilityHidden(true)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(verbatim: word.term)
                                Text(verbatim: CategoryTeachCopy.detail(word, teach: teach))
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            } header: {
                Text(verbatim: CategoryTeachCopy.question(teach))
                    .textCase(nil)
            } footer: {
                Text("Bills already saved don't change. You can undo it in the Category map.")
            }
        }
        .formStyle(.grouped)
        .navigationTitle("Teach the map")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", systemImage: "xmark", role: .cancel) {
                    model.send(GroupDetailUiEventOnTeachDismissed.shared)
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Teach", systemImage: "checkmark") {
                    model.send(GroupDetailUiEventOnTeachConfirmed.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(teach.selectedWord == nil || teach.isSaving)
            }
        }
        #if os(macOS)
        .frame(width: 460, height: 420)
        #endif
    }
}
