import UIKit
import UniformTypeIdentifiers

private let appGroup = "group.com.mettyoung.deconstructchinese"
private let sharedTextKey = "shared_text"
private let hostScheme = "deconstructchinese://shared"

/// Receives selected plain text from the share sheet, stashes it in the shared
/// App Group container, then opens the host app via its custom URL scheme.
class ShareViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        extractText { [weak self] text in
            guard let self = self else { return }
            if let text = text, !text.isEmpty {
                UserDefaults(suiteName: appGroup)?.set(text, forKey: sharedTextKey)
                self.openHostApp()
            }
            self.extensionContext?.completeRequest(returningItems: nil)
        }
    }

    private func extractText(completion: @escaping (String?) -> Void) {
        guard let item = extensionContext?.inputItems.first as? NSExtensionItem,
              let provider = item.attachments?.first else {
            completion(nil); return
        }
        let plainText = UTType.plainText.identifier
        if provider.hasItemConformingToTypeIdentifier(plainText) {
            provider.loadItem(forTypeIdentifier: plainText, options: nil) { data, _ in
                DispatchQueue.main.async { completion(data as? String) }
            }
        } else {
            completion(nil)
        }
    }

    private func openHostApp() {
        guard let url = URL(string: hostScheme) else { return }
        var responder: UIResponder? = self
        while let r = responder {
            if let app = r as? UIApplication {
                app.open(url, options: [:], completionHandler: nil)
                return
            }
            responder = r.next
        }
    }
}
