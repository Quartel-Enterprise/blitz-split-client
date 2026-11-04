# Joining and foldables

Invite links, joining, cameras, foldables and the iPhone Duo.

  - A group's invite link is `https://blitzsplit.com/g/<code>` (borarachar.com will redirect to blitzsplit.com).
    Joining through it lands on the group's Despesas (3.3, `Route.GroupDetail` with `joinedAs`) with "Você entrou como
    <nome>" over the bills and, while the account has no Google or Apple, "Proteja seu acesso", which opens the
    Perfil. "Convite" (3.1) and "Quem é você no grupo?" (3.2) are one column on a phone and upright on a tablet; lying
    down, on a tablet lying down (a 920 dp / 900 pt container) and on a book (one half each, 24 dp off the hinge's
    band) the title, or the question with "Não estou na lista", on the left and the group with its buttons, or the
    names with "Entrar como Bruno" always in sight, on the right; an Android tabletop puts the invite's title and
    group on top and "Entrar no grupo" and "Agora não" below ("Quem é você" is drawn like the book there). "Não estou
    na lista" is a bottom sheet on an Android phone, a 560 dp dialog on its tablets and foldables upright and in a
    book's right half, a sheet on iOS, a dialog on the web (a drawer on a phone). The web's "Quem é você" heads the
    names with the invite ("Convite de Pierre", "Entre no grupo Viagem Floripa"), beside the names from 1024 px, and pins
    "Entrar como Bruno" at the bottom of a phone. The Mac opens the invite as one sheet over the window, "Quem é você"
    pushed inside it. Joining with no profile on the device makes one from the name and
    color joined as. A member someone else already joined as says "Se era você, peça para <criador> liberar o nome em
    Membros" ("Se era você, fale com quem te chamou" once the creator's account is gone).
  - The home's "Entrar com link ou QR" opens the reader (`Route.JoinWithLink`) on every platform: the camera reads
    an invite's QR and keeps reading after a wrong one ("Esse QR não é um convite do Bora Rachar" for a few
    seconds), and "Tem o link?" under it takes a pasted or typed link. Only links on blitzsplit.com and
    borarachar.com count as invites there, since a QR can hold any site's address. Android and iOS open it full
    screen, macOS as a sheet and the web as a dialog (a drawer on a phone), whose camera reads through the browser's
    BarcodeDetector. An invite no longer valid offers "Escanear outro QR", which goes back to the reader; that reader
    ignores the QR of the invite that just failed, which may still be in front of the camera, so only another QR or
    a pasted link opens.
  - Android's "Tirar foto" asks for the camera first. Once it is refused for good, a snackbar "Permita a câmera nas
    configurações" offers "Configurações", which opens the app's settings page.
  - The web's "Tirar foto" (beta feedback 2): wherever the web takes a photo from a file (Seu perfil, Editar perfil,
    Novo grupo, Editar grupo, Conta's Anexos, Ler cupom fiscal) it also offers the browser's camera, a desktop's
    webcam too, but only while the browser lists a camera (`useCameras`). One dialog (`CameraCaptureDialog`, a
    drawer on a phone) shows it live, the selfie camera mirrored and the photo never, with the shutter, "Tirar outra"
    and "Usar foto", "Trocar câmera" with more than one (front and back where the cameras say which they are, else
    one camera after the other) and the flashlight where the browser has it; "Usar foto" hands the JPEG on as a file
    picked would be, so a profile's or a group's goes through "Ajustar foto". A camera refused says so ("Sem acesso
    à câmera") with "Tentar de novo" and "Escolher arquivo", and closing stops it, as does the photo taken.
  - Foldables and the iPhone Duo follow the fold the device reports, never the window alone (decision 8: Android's
    `FoldingFeature`, read as `FormFactor`; iOS 27.1's division region, `FoldDivision`), so a Galaxy Z Fold held as a
    book is a book: two halves meet on the hinge band (24 dp on Android) and a dialog the book draws opens in its
    right half with the other half under the scrim (`FoldDialog`), as the recovery code does on a book and on a
    tabletop lying down (there above its hinge, which runs across the right half too). First access splits there
    too: Boas-vindas' pitch, Seu perfil's avatar and Recuperar's accounts before the fold, what to tap after it, on
    a book, a tabletop lying down and an iPhone Duo open; the Duo closed drops Boas-vindas' benefits. Every Apple
    layout rule is the window's size (`ScreenLayout`), never the idiom, so an iPhone Duo open gets an iPad's list and
    detail and
    opens a key and a bill as sheets; its copy still says "iPhone". How each screen picks its layout:
    [docs/architecture/responsive-layouts.md](../architecture/responsive-layouts.md).
  - The iPhone Duo (iOS 27.1): closed, the system's vertical bar on the camera's edge holds Back, the screen's glass
    buttons, the avatar and "Rachar conta" (only its +, the group's Adicionar pessoa too) over the tabs
    (`prefersVerticalBar()`), and the group's bottom bar moves into it; open lying down the system keeps that bar.
    Open lying down, every two-column screen
    splits on the fold with nothing in its band (Grupos' and Contatos' lists end at it, Saldos, Estatísticas with
    the total and categories before it and Por pessoa after it), and a form sheet opens in the leading half
    (`foldSheetPlacement()`). Open upright, as a notebook, Convidar (QR and link on top), Convite (the group on top),
    the group (the header and "Seu saldo" on top, the tabs and what they hold below, on every tab), Registrar
    pagamento (who and how much on top, how and when below), Estatísticas (the total and Ao longo do tempo on top),
    Rachar conta (descrição and valor on top) and Dividir (the summary and modes on top, the people below) split on
    the fold (`FoldHalves`); everything else is an iPad's upright. Pix and Bitcoin keys stay a sheet in the leading
    half lying down, not the boards' two columns.
