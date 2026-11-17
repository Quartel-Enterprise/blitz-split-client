package com.quare.blitzsplit.android.storescreenshots

private val bannerCopies: Map<StoreScreen, Map<String, BannerCopy>> = mapOf(
    StoreScreen.GROUP to mapOf(
        "en" to ("Who paid what, at a glance" over "Every bill of the trip in one place, with your balance on top"),
        "pt" to ("Quem pagou o quê, num relance" over "Todas as contas da viagem num só lugar, seu saldo no topo"),
        "pt-PT" to
            ("Quem pagou o quê, num relance" over "Todas as contas da viagem num só sítio, com o teu saldo no topo"),
        "es" to
            ("Quién pagó qué, de un vistazo" over "Todos los gastos del viaje en un solo lugar, con tu saldo arriba"),
        "es-ES" to
            (
                "Quién ha pagado qué, de un vistazo" over
                    "Todos los gastos del viaje en un solo sitio, con tu saldo arriba"
            ),
        "fr" to
            (
                "Qui a payé quoi, en un coup d'œil" over
                    "Toutes les dépenses du voyage au même endroit, votre solde en haut"
            ),
        "de" to
            ("Wer was bezahlt hat, auf einen Blick" over "Alle Ausgaben der Reise an einem Ort, dein Saldo ganz oben"),
        "it" to
            (
                "Chi ha pagato cosa, a colpo d'occhio" over
                    "Tutte le spese del viaggio in un posto solo, con il tuo saldo in alto"
            ),
        "pl" to
            (
                "Kto za co zapłacił, w mgnieniu oka" over
                    "Wszystkie wydatki z wyjazdu w jednym miejscu, Twoje saldo na górze"
            ),
        "nl" to
            ("Wie wat betaalde, in één oogopslag" over "Alle uitgaven van de trip op één plek, met je saldo bovenaan"),
        "tr" to ("Kim neyi ödedi, tek bakışta" over "Tatilin tüm harcamaları tek yerde, bakiyen en üstte"),
        "id" to ("Siapa bayar apa, sekilas saja" over "Semua tagihan liburan di satu tempat, saldomu di paling atas"),
        "ru" to ("Кто за что платил — сразу видно" over "Все расходы поездки в одном месте, твой баланс сверху"),
        "uk" to ("Хто за що платив — одразу видно" over "Усі витрати подорожі в одному місці, твій баланс угорі"),
        "ja" to ("誰が何を払ったか、ひと目で" over "旅の支払いをまとめて表示。あなたの残高も一番上に"),
        "ko" to ("누가 뭘 냈는지 한눈에" over "여행 지출을 한곳에, 내 잔액은 맨 위에"),
        "hi" to ("किसने क्या दिया, एक नज़र में" over "ट्रिप के सारे खर्च एक जगह, आपका बैलेंस सबसे ऊपर"),
        "ar" to ("من دفع ماذا، بنظرة واحدة" over "كل مصروفات الرحلة في مكان واحد، ورصيدك في الأعلى"),
    ),
    StoreScreen.ITEMS to mapOf(
        "en" to ("Split by what each one had" over "Tick who had each item and the service charge splits fairly"),
        "pt" to
            ("Divida pelo que cada um consumiu" over "Marque quem pediu cada item e o serviço se divide na proporção"),
        "pt-PT" to
            ("Divide pelo que cada um consumiu" over "Marca quem pediu cada item e o serviço divide-se na proporção"),
        "es" to
            (
                "Divide según lo que pidió cada uno" over
                    "Marca quién pidió cada plato y el servicio se reparte en proporción"
            ),
        "es-ES" to
            (
                "Divide según lo que ha tomado cada uno" over
                    "Marca quién ha pedido cada plato y el servicio se reparte en proporción"
            ),
        "fr" to
            (
                "Partagez selon ce que chacun a pris" over
                    "Cochez qui a pris chaque plat, le service se répartit au prorata"
            ),
        "de" to
            ("Teilen nach dem, was jeder hatte" over "Hak ab, wer was hatte, und der Service wird anteilig verteilt"),
        "it" to
            (
                "Dividi in base a cosa ha preso ognuno" over
                    "Segna chi ha preso ogni piatto e il servizio si divide in proporzione"
            ),
        "pl" to
            ("Podziel według tego, co kto zamówił" over "Zaznacz, kto co jadł, a serwis rozłoży się proporcjonalnie"),
        "nl" to ("Verdeel naar wat ieder had" over "Vink aan wie wat had en de service wordt eerlijk verdeeld"),
        "tr" to ("Herkes ne yediyse onu ödesin" over "Her ürünü kimin aldığını işaretle, servis adilce bölünsün"),
        "id" to ("Bagi sesuai pesanan masing-masing" over "Tandai siapa pesan apa, biaya layanan terbagi adil"),
        "ru" to ("Каждый платит за своё" over "Отметь, кто что ел, и сервисный сбор разделится честно"),
        "uk" to ("Кожен платить за своє" over "Познач, хто що їв, і сервісний збір розділиться чесно"),
        "ja" to ("注文したものごとに割り勘" over "誰が何を頼んだかを選ぶだけ。サービス料も公平に分配"),
        "ko" to ("각자 먹은 만큼 나눠요" over "누가 뭘 먹었는지 고르면 봉사료도 공평하게"),
        "hi" to ("जिसने जो लिया, वही चुकाए" over "चुनें कि हर आइटम किसने लिया, सर्विस चार्ज भी बराबर बंटेगा"),
        "ar" to ("كلٌّ يدفع ما طلبه" over "حدّد من طلب كل عنصر وتُقسَّم رسوم الخدمة بإنصاف"),
    ),
    StoreScreen.RECEIPT to mapOf(
        "en" to ("Snap the receipt, skip the typing" over "The items fill themselves in from a photo"),
        "pt" to ("Fotografe o cupom e pronto" over "Os itens se preenchem sozinhos a partir da foto"),
        "pt-PT" to ("Fotografa o talão e já está" over "Os itens preenchem-se sozinhos a partir da foto"),
        "es" to ("Saca una foto al ticket y listo" over "Los platos se completan solos a partir de la foto"),
        "es-ES" to ("Haz una foto al ticket y listo" over "Los platos se rellenan solos a partir de la foto"),
        "fr" to
            ("Photographiez le ticket, c'est tout" over "Les articles se remplissent tout seuls à partir de la photo"),
        "de" to ("Beleg fotografieren, fertig" over "Die Positionen füllen sich aus dem Foto von selbst"),
        "it" to ("Fotografa lo scontrino ed è fatta" over "Le voci si compilano da sole a partire dalla foto"),
        "pl" to ("Zrób zdjęcie paragonu i gotowe" over "Pozycje uzupełnią się same ze zdjęcia"),
        "nl" to ("Maak een foto van de bon, klaar" over "De items vullen zich vanzelf in vanaf de foto"),
        "tr" to ("Fişin fotoğrafını çek, yeter" over "Ürünler fotoğraftan kendiliğinden dolsun"),
        "id" to ("Foto struknya, beres" over "Daftar item terisi sendiri dari foto"),
        "ru" to ("Сфотографируй чек — и всё" over "Позиции заполнятся сами по фото"),
        "uk" to ("Сфотографуй чек — і все" over "Позиції заповняться самі з фото"),
        "ja" to ("レシートを撮るだけ" over "写真から品目が自動で入力されます"),
        "ko" to ("영수증은 찍기만 하세요" over "사진에서 항목이 자동으로 채워져요"),
        "hi" to ("रसीद की फ़ोटो लें, टाइप करना भूल जाएं" over "फ़ोटो से आइटम अपने-आप भर जाते हैं"),
        "ar" to ("صوّر الإيصال بلا كتابة" over "تُملأ العناصر تلقائيًا من الصورة"),
    ),
    StoreScreen.BALANCES to mapOf(
        "en" to ("Know who owes whom" over "Settle up in a tap and record the payment"),
        "pt" to ("Saiba quem deve a quem" over "Acerte em um toque e registre o pagamento"),
        "pt-PT" to ("Fica a saber quem deve a quem" over "Acerta com um toque e regista o pagamento"),
        "es" to ("Sabe quién le debe a quién" over "Salda cuentas con un toque y registra el pago"),
        "es-ES" to ("Mira quién debe a quién" over "Salda cuentas con un toque y registra el pago"),
        "fr" to ("Sachez qui doit à qui" over "Réglez en un geste et enregistrez le paiement"),
        "de" to ("Wisse, wer wem was schuldet" over "Mit einem Tipp begleichen und die Zahlung eintragen"),
        "it" to ("Sai sempre chi deve a chi" over "Salda con un tocco e registra il pagamento"),
        "pl" to ("Wiesz, kto komu ile jest winien" over "Rozlicz się jednym dotknięciem i zapisz płatność"),
        "nl" to ("Weet wie wie wat schuldig is" over "Reken af met één tik en leg de betaling vast"),
        "tr" to ("Kim kime borçlu, hep bil" over "Tek dokunuşla hesaplaş, ödemeyi kaydet"),
        "id" to ("Tahu siapa berutang ke siapa" over "Lunasi sekali ketuk dan catat pembayarannya"),
        "ru" to ("Знай, кто кому должен" over "Рассчитайся в одно касание и запиши платёж"),
        "uk" to ("Знай, хто кому винен" over "Розрахуйся в один дотик і запиши платіж"),
        "ja" to ("誰が誰にいくら、すぐわかる" over "ワンタップで精算して支払いを記録"),
        "ko" to ("누가 누구에게 얼마인지" over "탭 한 번으로 정산하고 송금을 기록해요"),
        "hi" to ("किसे किसको कितना देना है" over "एक टैप में हिसाब चुकाएं और भुगतान दर्ज करें"),
        "ar" to ("اعرف من عليه لمن" over "تسوية بلمسة واحدة وتسجيل الدفعة"),
    ),
    StoreScreen.INVITE to mapOf(
        "en" to ("Invite with a link or QR code" over "Friends join the group in seconds, no sign-up needed"),
        "pt" to ("Convide por link ou QR code" over "A galera entra no grupo em segundos, sem cadastro"),
        "pt-PT" to ("Convida por link ou QR code" over "Os amigos entram no grupo em segundos, sem criar conta"),
        "es" to ("Invita con un enlace o código QR" over "Tus amigos se unen en segundos, sin registrarse"),
        "fr" to
            ("Invitez par lien ou QR code" over "Vos amis rejoignent le groupe en quelques secondes, sans inscription"),
        "de" to ("Per Link oder QR-Code einladen" over "Freunde treten in Sekunden bei, ganz ohne Anmeldung"),
        "it" to
            ("Invita con un link o un codice QR" over "Gli amici entrano nel gruppo in pochi secondi, senza account"),
        "pl" to ("Zaproś linkiem lub kodem QR" over "Znajomi dołączą w kilka sekund, bez rejestracji"),
        "nl" to ("Nodig uit met een link of QR-code" over "Vrienden doen in seconden mee, zonder account"),
        "tr" to ("Bağlantıyla ya da QR kodla davet et" over "Arkadaşların saniyeler içinde katılır, kayıt gerekmez"),
        "id" to ("Undang lewat tautan atau kode QR" over "Teman bergabung dalam hitungan detik, tanpa daftar"),
        "ru" to ("Пригласи по ссылке или QR-коду" over "Друзья присоединятся за секунды, без регистрации"),
        "uk" to ("Запроси за посиланням або QR-кодом" over "Друзі приєднаються за секунди, без реєстрації"),
        "ja" to ("リンクやQRコードで招待" over "友だちは登録不要、数秒でグループに参加"),
        "ko" to ("링크나 QR 코드로 초대" over "가입 없이 몇 초 만에 그룹에 참여해요"),
        "hi" to
            ("लिंक या QR कोड से आमंत्रित करें" over "दोस्त कुछ ही सेकंड में ग्रुप से जुड़ें, साइन अप की ज़रूरत नहीं"),
        "ar" to ("دعوة برابط أو رمز QR" over "ينضم الأصدقاء إلى المجموعة في ثوانٍ، دون تسجيل"),
    ),
    StoreScreen.STATISTICS to mapOf(
        "en" to ("See where the money went" over "Spending by category and by person"),
        "pt" to ("Veja para onde foi o dinheiro" over "Gastos por categoria e por pessoa"),
        "pt-PT" to ("Vê para onde foi o dinheiro" over "Gastos por categoria e por pessoa"),
        "es" to ("Mira en qué se fue el dinero" over "Gastos por categoría y por persona"),
        "es-ES" to ("Mira en qué se ha ido el dinero" over "Gastos por categoría y por persona"),
        "fr" to ("Voyez où est passé l'argent" over "Dépenses par catégorie et par personne"),
        "de" to ("Sieh, wohin das Geld ging" over "Ausgaben nach Kategorie und Person"),
        "it" to ("Scopri dove sono finiti i soldi" over "Spese per categoria e per persona"),
        "pl" to ("Zobacz, na co poszły pieniądze" over "Wydatki według kategorii i osób"),
        "nl" to ("Zie waar het geld heen ging" over "Uitgaven per categorie en per persoon"),
        "tr" to ("Paranın nereye gittiğini gör" over "Kategoriye ve kişiye göre harcamalar"),
        "id" to ("Lihat ke mana uangnya pergi" over "Pengeluaran per kategori dan per orang"),
        "ru" to ("Смотри, куда ушли деньги" over "Расходы по категориям и по людям"),
        "uk" to ("Дивись, куди пішли гроші" over "Витрати за категоріями та людьми"),
        "ja" to ("お金の行き先がわかる" over "カテゴリ別・人別の支出"),
        "ko" to ("돈이 어디로 갔는지 확인" over "카테고리별, 사람별 지출"),
        "hi" to ("देखें पैसा कहां गया" over "श्रेणी और व्यक्ति के हिसाब से खर्च"),
        "ar" to ("أين ذهبت الأموال" over "الإنفاق حسب الفئة وحسب الشخص"),
    ),
    StoreScreen.HOME to mapOf(
        "en" to ("Your groups, front and center" over "Pin the ones you use most and see your balance in each"),
        "pt" to ("Seus grupos sempre à mão" over "Fixe os que você mais usa e veja seu saldo em cada um"),
        "pt-PT" to ("Os teus grupos sempre à mão" over "Fixa os que mais usas e vê o teu saldo em cada um"),
        "es" to ("Tus grupos, siempre a mano" over "Fija los que más usas y mira tu saldo en cada uno"),
        "fr" to ("Vos groupes, toujours sous la main" over "Épinglez vos favoris et voyez votre solde dans chacun"),
        "de" to ("Deine Gruppen immer griffbereit" over "Pinne die wichtigsten an und sieh deinen Saldo in jeder"),
        "it" to
            ("I tuoi gruppi sempre a portata di mano" over "Fissa quelli che usi di più e vedi il tuo saldo in ognuno"),
        "pl" to ("Twoje grupy zawsze pod ręką" over "Przypnij najczęstsze i sprawdzaj saldo w każdej"),
        "nl" to ("Je groepen altijd bij de hand" over "Zet je favorieten vast en zie je saldo per groep"),
        "tr" to ("Grupların hep el altında" over "En çok kullandıklarını sabitle, her birinde bakiyeni gör"),
        "id" to ("Grupmu selalu di depan" over "Sematkan yang sering dipakai dan lihat saldomu di tiap grup"),
        "ru" to ("Твои группы всегда под рукой" over "Закрепи самые нужные и смотри баланс в каждой"),
        "uk" to ("Твої групи завжди під рукою" over "Закріпи найпотрібніші й дивись баланс у кожній"),
        "ja" to ("よく使うグループをすぐ開ける" over "ピン留めして、グループごとの残高を確認"),
        "ko" to ("자주 쓰는 그룹을 바로" over "고정하고 그룹마다 내 잔액을 확인해요"),
        "hi" to
            (
                "आपके ग्रुप, हमेशा सामने" over
                    "सबसे ज़्यादा इस्तेमाल होने वाले ग्रुप पिन करें और हर एक में अपना बैलेंस देखें"
            ),
        "ar" to ("مجموعاتك في المقدمة" over "ثبّت الأكثر استخدامًا واطّلع على رصيدك في كل منها"),
    ),
    StoreScreen.PAYERS_AND_CURRENCIES to mapOf(
        "en" to ("Several payers, any currency" over "Split bills more than one person paid, even in crypto"),
        "pt" to ("Vários pagantes, qualquer moeda" over "Divida contas pagas por mais de uma pessoa, até em cripto"),
        "pt-PT" to
            ("Vários pagadores, qualquer moeda" over "Divide contas pagas por mais de uma pessoa, até em cripto"),
        "es" to
            ("Varios pagadores, cualquier moneda" over "Divide gastos que pagaron varias personas, incluso en cripto"),
        "es-ES" to
            (
                "Varios pagadores, cualquier moneda" over
                    "Divide gastos que han pagado varias personas, incluso en cripto"
            ),
        "fr" to ("Plusieurs payeurs, toutes les devises" over "Partagez une note réglée à plusieurs, même en crypto"),
        "de" to ("Mehrere Zahler, jede Währung" over "Teile Rechnungen, die mehrere bezahlt haben, sogar in Krypto"),
        "it" to ("Più paganti, qualsiasi valuta" over "Dividi spese pagate da più persone, anche in cripto"),
        "pl" to
            ("Kilku płacących, dowolna waluta" over "Dziel rachunki opłacone przez kilka osób, nawet w kryptowalucie"),
        "nl" to
            ("Meerdere betalers, elke valuta" over "Verdeel rekeningen die meerdere mensen betaalden, zelfs in crypto"),
        "tr" to ("Birden çok ödeyen, her para birimi" over "Birkaç kişinin ödediği hesapları böl, kriptoda bile"),
        "id" to
            ("Banyak pembayar, mata uang apa pun" over "Bagi tagihan yang dibayar beberapa orang, bahkan dalam kripto"),
        "ru" to
            ("Несколько плательщиков, любая валюта" over "Дели счета, оплаченные несколькими людьми, даже в крипте"),
        "uk" to
            ("Кілька платників, будь-яка валюта" over "Діли рахунки, сплачені кількома людьми, навіть у криптовалюті"),
        "ja" to ("複数人の支払いも、どの通貨でも" over "何人かで払った会計も分けられる。暗号資産にも対応"),
        "ko" to ("여러 명이 내도, 어떤 통화든" over "여럿이 낸 지출도 나눠요. 암호화폐도 돼요"),
        "hi" to ("कई भुगतानकर्ता, कोई भी मुद्रा" over "कई लोगों के चुकाए खर्च भी बांटें, क्रिप्टो में भी"),
        "ar" to ("أكثر من دافع، وأي عملة" over "تقسيم فواتير دفعها أكثر من شخص، حتى بالعملات المشفرة"),
    ),
    StoreScreen.ACTIVITY to mapOf(
        "en" to ("Never miss a change" over "Every new bill and payment in your groups, as it happens"),
        "pt" to ("Não perca nenhuma novidade" over "Cada conta e pagamento novo dos seus grupos, na hora"),
        "pt-PT" to ("Não percas nenhuma novidade" over "Cada conta e pagamento novo dos teus grupos, na hora"),
        "es" to ("No te pierdas ningún cambio" over "Cada gasto y pago nuevo de tus grupos, al instante"),
        "fr" to ("Ne manquez aucun changement" over "Chaque dépense et paiement de vos groupes, en direct"),
        "de" to ("Keine Änderung verpassen" over "Jede neue Ausgabe und Zahlung deiner Gruppen, sofort"),
        "it" to ("Non perderti nessuna modifica" over "Ogni nuova spesa e pagamento dei tuoi gruppi, in tempo reale"),
        "pl" to ("Nie przegap żadnej zmiany" over "Każdy nowy wydatek i płatność w Twoich grupach, na bieżąco"),
        "nl" to ("Mis geen enkele wijziging" over "Elke nieuwe uitgave en betaling in je groepen, meteen"),
        "tr" to ("Hiçbir değişikliği kaçırma" over "Gruplarındaki her yeni harcama ve ödeme, anında"),
        "id" to ("Jangan lewatkan perubahan" over "Setiap tagihan dan pembayaran baru di grupmu, langsung"),
        "ru" to ("Не пропусти ни одного изменения" over "Каждый новый расход и платёж в твоих группах — сразу"),
        "uk" to ("Не пропусти жодної зміни" over "Кожна нова витрата й платіж у твоїх групах — одразу"),
        "ja" to ("変更を見逃さない" over "グループの新しい支払いや精算をすぐにお知らせ"),
        "ko" to ("변경 사항을 놓치지 마세요" over "그룹의 새 지출과 송금을 바로 알려 드려요"),
        "hi" to ("कोई बदलाव न छूटे" over "आपके ग्रुप का हर नया खर्च और भुगतान, उसी समय"),
        "ar" to ("لا يفوتك أي تغيير" over "كل فاتورة ودفعة جديدة في مجموعاتك، لحظة حدوثها"),
    ),
    StoreScreen.LIGHT_THEME to mapOf(
        "en" to ("Light or dark, it follows you" over "Works offline, with or without an account"),
        "pt" to ("Claro ou escuro, do seu jeito" over "Funciona offline, com ou sem conta"),
        "pt-PT" to ("Claro ou escuro, à tua maneira" over "Funciona offline, com ou sem conta"),
        "es" to ("Claro u oscuro, a tu gusto" over "Funciona sin conexión, con o sin cuenta"),
        "fr" to ("Clair ou sombre, à votre goût" over "Fonctionne hors ligne, avec ou sans compte"),
        "de" to ("Hell oder dunkel, wie du willst" over "Funktioniert offline, mit oder ohne Konto"),
        "it" to ("Chiaro o scuro, come preferisci" over "Funziona offline, con o senza account"),
        "pl" to ("Jasny lub ciemny, jak wolisz" over "Działa offline, z kontem lub bez"),
        "nl" to ("Licht of donker, zoals jij wilt" over "Werkt offline, met of zonder account"),
        "tr" to ("Açık ya da koyu, sana göre" over "Çevrimdışı çalışır, hesaplı ya da hesapsız"),
        "id" to ("Terang atau gelap, sesukamu" over "Bisa offline, dengan atau tanpa akun"),
        "ru" to ("Светлая или тёмная — как тебе нравится" over "Работает офлайн, с аккаунтом или без"),
        "uk" to ("Світла чи темна — як тобі подобається" over "Працює офлайн, з обліковим записом або без"),
        "ja" to ("ライトもダークも、お好みで" over "オフラインでも、アカウントなしでも使えます"),
        "ko" to ("라이트도 다크도 내 마음대로" over "오프라인에서도, 계정이 없어도 돼요"),
        "hi" to ("लाइट या डार्क, जैसा आप चाहें" over "ऑफ़लाइन भी चलता है, अकाउंट के साथ या उसके बिना"),
        "ar" to ("فاتح أو داكن، كما تحب" over "يعمل دون اتصال، بحساب أو من دونه"),
    ),
)

internal fun bannerCopyOf(
    screen: StoreScreen,
    language: String,
): BannerCopy {
    val copies = bannerCopies.getValue(screen)
    // A regional language (es-ES) only lists the banners it words differently, as the apps' regional strings do.
    return copies[language] ?: copies[language.substringBefore('-')] ?: copies.getValue("en")
}

private infix fun String.over(description: String): BannerCopy = BannerCopy(
    title = this,
    description = description,
)
