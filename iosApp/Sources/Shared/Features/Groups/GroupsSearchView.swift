import Shared
import SwiftUI

/// "Search" (5.2): the groups, people and expenses on this device, the settled groups included, offline too. The
/// iPhone and the iPad show it in the search tab, under a large title with the field and All / Groups / People /
/// Expenses; the Mac in the groups' place while its toolbar field has text. A group found opens the group, a person
/// the members of the group they were found in, an expense its bill over the group. Always a list, even with the grid.
struct GroupsSearchResultsList: View {
    let model: GroupsModel

    var body: some View {
        let search = model.state.search
        if search.showsNoResults {
            ContentUnavailableView {
                Label {
                    Text("Nothing for “\(search.trimmedQuery)”")
                } icon: {
                    Image(systemName: "magnifyingglass")
                }
            } description: {
                Text("We searched the groups, people and expenses on this \(DeviceName.current), settled groups included.")
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if !search.hasQuery {
            Text("Searches the groups, people and expenses on this \(DeviceName.current), settled groups included, even without internet.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
                .padding(.top, 48)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        } else {
            resultsList(search.results)
        }
    }

    private func resultsList(_ results: GroupsSearchResults) -> some View {
        List {
            if !results.groups.isEmpty {
                Section("Groups") {
                    ForEach(results.groups, id: \.group.id) { hit in
                        Button {
                            model.send(GroupsUiEventOnGroupClicked(groupId: hit.group.id))
                        } label: {
                            SearchGroupRow(hit: hit)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            if !results.people.isEmpty {
                Section("People") {
                    ForEach(results.people, id: \.self) { hit in
                        Button {
                            model.send(GroupsUiEventOnSearchPersonClicked(groupId: hit.groupId))
                        } label: {
                            SearchPersonRow(hit: hit)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            if !results.bills.isEmpty {
                Section("Expenses") {
                    ForEach(results.bills, id: \.id) { hit in
                        Button {
                            model.send(GroupsUiEventOnSearchBillClicked(groupId: hit.groupId, expenseId: hit.id))
                        } label: {
                            SearchBillRow(hit: hit)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        #if os(iOS)
        .listStyle(.insetGrouped)
        #else
        .listStyle(.inset(alternatesRowBackgrounds: true))
        #endif
    }
}

#if os(iOS)
/// The search tab: a large "Search" title, the field under it, and All / Groups / People / Expenses as segments.
struct GroupsSearchView: View {
    let model: GroupsModel

    var body: some View {
        VStack(spacing: 0) {
            Picker("Search in", selection: GroupsSearchCopy.scopeBinding(model)) {
                ForEach(GroupsSearchCopy.scopes, id: \.self) { scope in
                    Text(GroupsSearchCopy.scopeTitle(scope)).tag(scope)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
            GroupsSearchResultsList(model: model)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .navigationTitle("Search")
        .searchable(
            text: GroupsSearchCopy.queryBinding(model),
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: Text("Groups, people and expenses"),
        )
    }
}
#endif

/// The words and bindings of the search, the same on iPhone, iPad and Mac.
enum GroupsSearchCopy {
    static let scopes: [SearchScope] = [.all, .groups, .people, .bills]

    static func scopeTitle(_ scope: SearchScope) -> LocalizedStringKey {
        switch scope {
        case .groups: "Groups"
        case .people: "People"
        case .bills: "Expenses"
        default: "All"
        }
    }

    /// What is typed: the field reads the ViewModel's text back at once, so the caret never jumps.
    @MainActor static func queryBinding(_ model: GroupsModel) -> Binding<String> {
        Binding(
            get: { model.current.search.query },
            set: { model.send(GroupsUiEventOnSearchQueryChanged(query: $0)) },
        )
    }

    @MainActor static func scopeBinding(_ model: GroupsModel) -> Binding<SearchScope> {
        Binding(
            get: { model.state.search.scope },
            set: { model.send(GroupsUiEventOnSearchScopeChanged(scope: $0)) },
        )
    }

    /// The text with what the search found in bold, as the canvas marks "Chur" in "Churrasco". The match counts
    /// UTF-16 units, as Kotlin does.
    static func marked(_ text: String, _ match: TextMatch?) -> AttributedString {
        marked(text, from: match.map { Int($0.start) }, to: match.map { Int($0.end) })
    }

    /// The same mark from where the match starts and ends, in UTF-16 units: Contatos' search marks names too.
    static func marked(_ text: String, from start: Int?, to end: Int?) -> AttributedString {
        var result = AttributedString(text)
        guard let start, let end, start >= 0, start < end, end <= text.utf16.count else {
            return result
        }
        let lower = String.Index(utf16Offset: start, in: text)
        let upper = String.Index(utf16Offset: end, in: text)
        if let range = Range(lower..<upper, in: result) {
            result[range].inlinePresentationIntent = .stronglyEmphasized
        }
        return result
    }

    /// "9 members · 3 days"; a settled group's detail already counts its members.
    static func groupMeta(_ row: GroupRow) -> String {
        if row.detail == .everyoneSettled { return GroupsCopy.detail(row) }
        return "\(GroupsCopy.members(row.memberCount)) · \(GroupsCopy.detail(row))"
    }

    /// "Weekend barbecue · Bruno paid", or only the group when nobody in it paid.
    static func billMeta(_ hit: SearchBillHit) -> String {
        let payers = hit.payers.map { (name: $0.name, isMe: $0.isMe) }
        guard let paid = ExpenseFormat.paid(payers, loneFormer: nil) else { return hit.groupName }
        return "\(hit.groupName) · \(paid)"
    }
}

private struct SearchGroupRow: View {
    let hit: SearchGroupHit

    var body: some View {
        let group = hit.group
        HStack(spacing: 12) {
            GroupAvatar(initials: group.initials, color: group.color, size: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(GroupsSearchCopy.marked(group.name, hit.nameMatch))
                    .lineLimit(2)
                Group {
                    if let bill = hit.billDescription {
                        Text("Settled up · expense “\(Text(GroupsSearchCopy.marked(bill, hit.billMatch)))”")
                    } else {
                        Text(verbatim: GroupsSearchCopy.groupMeta(group))
                    }
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            }
            Spacer(minLength: 8)
            if group.isSettled {
                Text("Settled up")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                Text(verbatim: group.currency.formatSigned(group.netMinor))
                    .font(.subheadline.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(Palette.balance(group.netMinor))
            }
        }
        .contentShape(.rect)
    }
}

private struct SearchPersonRow: View {
    let hit: SearchPersonHit

    var body: some View {
        HStack(spacing: 12) {
            AvatarPhoto(initial: hit.initial, color: hit.color, photoUrl: nil, size: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(GroupsSearchCopy.marked(hit.name, hit.nameMatch))
                    .lineLimit(1)
                Text(verbatim: hit.groupName)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
        }
        .contentShape(.rect)
    }
}

private struct SearchBillRow: View {
    let hit: SearchBillHit

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "receipt")
                .foregroundStyle(.secondary)
                .frame(width: 36, height: 36)
                .background(.fill.tertiary, in: .rect(cornerRadius: 10))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(GroupsSearchCopy.marked(hit.description_, hit.descriptionMatch))
                    .lineLimit(2)
                Text(verbatim: GroupsSearchCopy.billMeta(hit))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            Text(verbatim: hit.currency.format(hit.totalMinor))
                .font(.subheadline)
                .monospacedDigit()
        }
        .contentShape(.rect)
    }
}
