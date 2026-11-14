import Shared
import SwiftUI
import UniformTypeIdentifiers

/// The words of "Export group data": the format picker's, and the file's headers, which the shared code writes with
/// them in the app's language.
enum GroupExportCopy {
    static var words: GroupExportWords {
        GroupExportWords(
            bills: String(localized: "Expenses (an export's sheet)", defaultValue: "Expenses", bundle: .appLanguage),
            payments: String(localized: "Payments (an export's sheet)", defaultValue: "Payments", bundle: .appLanguage),
            balances: String(localized: "Balances (an export's sheet)", defaultValue: "Balances", bundle: .appLanguage),
            date: String(localized: "Date (an export's column)", defaultValue: "Date", bundle: .appLanguage),
            billDescription: String(
                localized: "Description (an export's column)",
                defaultValue: "Description",
                bundle: .appLanguage,
            ),
            category: String(localized: "Category (an export's column)", defaultValue: "Category", bundle: .appLanguage),
            amount: String(localized: "Amount (an export's column)", defaultValue: "Amount", bundle: .appLanguage),
            currency: String(localized: "Currency (an export's column)", defaultValue: "Currency", bundle: .appLanguage),
            paid: String(localized: "Paid (an export's column)", defaultValue: "Paid", bundle: .appLanguage),
            share: String(localized: "Share (an export's column)", defaultValue: "Share", bundle: .appLanguage),
            from: String(localized: "From (an export's column)", defaultValue: "From", bundle: .appLanguage),
            to: String(localized: "To (an export's column)", defaultValue: "To", bundle: .appLanguage),
            member: String(localized: "Member (an export's column)", defaultValue: "Member", bundle: .appLanguage),
            balance: String(localized: "Balance (an export's column)", defaultValue: "Balance", bundle: .appLanguage),
            formerMember: String(localized: "Former member", bundle: .appLanguage),
            appCategoryNames: Dictionary(
                uniqueKeysWithValues: AppCategory.entries.map { ($0.key, CategoryCopy.name($0)) },
            ),
        )
    }

    static func label(_ format: GroupExportFormat) -> String {
        switch format {
        case .csv: "CSV"
        case .xlsx: "Excel (XLSX)"
        case .json: "JSON"
        default: String(localized: "Text (TXT)", bundle: .appLanguage)
        }
    }

    nonisolated static func contentType(_ format: GroupExportFormat) -> UTType {
        switch format {
        case .csv: .commaSeparatedText
        case .xlsx: UTType(filenameExtension: "xlsx") ?? .data
        case .json: .json
        default: .plainText
        }
    }
}

/// The file "Export group data" made, as the document the file exporter writes. Nonisolated, since the exporter
/// writes the file off the main actor.
nonisolated struct GroupExportDocument: FileDocument {
    /// Every type the export writes, so the exporter can save each one as itself.
    static let readableContentTypes: [UTType] = GroupExportFormat.entries.map(GroupExportCopy.contentType)

    let data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else { throw CocoaError(.fileReadCorruptFile) }
        self.data = data
    }

    func fileWrapper(configuration _: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

extension View {
    /// "Export group data": the formats to pick from (an action sheet on iPhone, a dialog on iPad and the Mac), then
    /// the save sheet (the save panel on the Mac) with the file the ViewModel made, and a toast when it couldn't.
    func groupExport(_ model: GroupDetailModel?) -> some View {
        let file = model?.exportFile
        return confirmationDialog(
            "Export group data",
            isPresented: Binding(
                get: { model?.state.showsExportFormats == true },
                set: { isPresented in
                    if !isPresented { model?.dismissExport() }
                },
            ),
            titleVisibility: .visible,
        ) {
            ForEach(GroupExportFormat.entries, id: \.self) { format in
                Button(GroupExportCopy.label(format)) { model?.export(format) }
            }
            Button("Cancel", role: .cancel) { model?.dismissExport() }
        }
        .fileExporter(
            isPresented: Binding(
                get: { model?.exportFile != nil },
                set: { isPresented in
                    if !isPresented { model?.exportFile = nil }
                },
            ),
            document: file.map { GroupExportDocument(data: BlitzSplitApple.shared.toData(bytes: $0.bytes) as Data) },
            contentType: file.map { GroupExportCopy.contentType($0.format) } ?? .data,
            defaultFilename: file?.fileName,
        ) { _ in
            model?.exportFile = nil
        }
        .glassToast("Couldn't export the group's data. Try again.", trigger: model?.exportErrorCount ?? 0)
    }
}
