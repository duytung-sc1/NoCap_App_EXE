# NoCap Privacy Policy

**Effective date:** September 24, 2026

**App:** NoCap for Android (`com.nocap.app`)

**Privacy contact:** dp1.1a9.tung@gmail.com

This policy describes how NoCap handles information when you use the Android app and its connected account, synchronization, storage, and payment services. The Vietnamese version follows the English version below.

## 1. What NoCap does

NoCap lets you read books and documents, save reading progress and annotations, and optionally use an account for eligible cloud features. You can use local reading features as a guest. An account is needed for account-based synchronization, private cloud storage, backup, and payment or plan management.

## 2. Data stored on your device

When you import a file or receive one from another app, NoCap reads the file you selected and stores a copy in its app storage for reading. Local data can include downloaded books, imported documents, reading position, bookmarks, highlights, notes, tags, collections, review items, reading sessions, and reading preferences. Using the file picker or an “Open with” action does not give NoCap access to every file on your device.

Local reading data is not automatically public. It can be sent to NoCap's servers when you sign in and use an eligible sync, private-document, or cloud-backup feature. The data sent depends on that feature; simply importing a file does not publish it to other users.

**Android system backup is separate from NoCap cloud backup.** Depending on your Android backup and device-transfer settings, the operating system may copy some local app data to your Google account or another device. NoCap excludes its saved sign-in session and entitlement snapshot files from this system backup, but other local app data may be included. You can manage system backups in your device or Google account settings.

## 3. Information handled when you use an account

- **Identity and profile:** Email address, display name, optional profile-photo URL, email-verification status, account status, and account creation or recent sign-in time. If you choose Google Sign-In, the app sends a Google identity token to the NoCap server for verification; the server may keep the linked Google account identifier. If you use an email-and-password account, the server stores a salted password hash rather than your plaintext password.
- **Sessions and security:** NoCap issues a session token to your app and stores only a hash of that token on the server. Sessions normally expire after up to 30 days and can be revoked by signing out or deleting the account. The service uses temporary rate-limit records and request information, including an IP address supplied to the service, to reduce abuse. Its infrastructure provider may also process standard network and operational logs.
- **Devices:** To identify a device for account and sync functions, the service stores an installation identifier, platform, device model, app version, registration time, and recent contact time. It does not need your contacts, precise location, microphone, or camera for its current reading features.
- **Reading and cloud data:** If a cloud feature is available to your account and you use it, NoCap can store synchronized reading progress, library metadata, bookmarks, highlights, notes, tags, collections, review items, reading-session records, private document files, and cloud-backup copies. These records are associated with your account so your devices can retrieve them. Private files are not placed in the public book catalog.
- **Plan and administrative data:** The service stores your Free/Pro plan, entitlement status and expiry, and related purchase or administrator-issued test-access metadata. Authorized administrators can view account identifiers, plan status, aggregate storage and technical sync information needed to operate the service. The admin dashboard is designed to show technical conflict metadata rather than the contents of private files or notes.

## 4. Bank-transfer payments

If you request a bank-transfer order for Pro, NoCap creates a payment code and stores the order amount, currency, duration, status, timestamps, and the account to which the order belongs. SePay sends a signed transaction notification to the server. To match a payment and prevent duplicate processing, NoCap stores limited transaction metadata such as the transaction identifier, amount, date, payment code, reference code, and a hash of the received event. The server does not store the full bank-transfer description, your banking password, or payment-card numbers. Your banking app and SePay handle the payment under their own privacy practices. Real Google Play purchases are not configured in the current Android build; this policy must be reviewed if they are enabled later.

## 5. Why we use this information

We use information to sign you in, verify your email, reset a password when requested, provide reading and cloud features, synchronize eligible data across your devices, manage storage and backups, verify payments and plan access, prevent abuse, investigate service errors, and answer support or privacy requests. The current Android app does not include an advertising or third-party analytics SDK. We do not sell personal information.

## 6. When information goes to other services

- **Cloudflare** operates NoCap's API, database, and private object storage, and may process network or operational information needed to run those services.
- **Google** processes information when you choose Google Sign-In. Android's own backup or device-transfer service may process local app data if you have enabled it; this is separate from NoCap's cloud storage.
- **SePay and your bank** process a transfer when you choose bank-transfer payment. NoCap receives only the transaction information needed to reconcile the order.
- **The configured email service** delivers account-verification and password-reset messages to your email address.
- **External book and cover providers** may receive ordinary request information, such as an IP address, when a file or image is loaded from their servers.

These providers may process information in countries other than your own and apply their own privacy terms. We do not intentionally make your private documents, notes, or cloud backups publicly accessible.

## 7. How long information is kept

- **On-device data:** Until you delete the item, clear the app's data, or remove the app, subject to any copies kept by Android system backup or device transfer.
- **Account and cloud data:** While the account exists, or until you delete an individual item or backup. An expired Pro plan does not by itself delete your local files and annotations.
- **Sessions and short-lived security records:** Sessions expire after up to 30 days; expired session, password-action, and rate-limit records are removed by scheduled server cleanup. Signing out clears the saved session on your device and attempts to revoke the server session. If the service is unreachable, the server session can remain valid until it expires.
- **Payment metadata:** Payment orders linked to an account are removed with that account. Limited SePay transaction-event metadata can remain after account deletion for payment reconciliation, duplicate-event prevention, fraud investigation, or applicable legal requirements. The current service has no automatic expiry period for those event records; contact us if you want us to review a deletion request where permitted.
- **Third-party copies:** Google, your bank, SePay, external book providers, and infrastructure providers may retain information under their own policies.

## 8. Your choices and deletion requests

You may read locally as a guest, choose whether to sign in with Google, delete individual local books or cloud backups where the app offers that action, and sign out to clear the saved session on your device. To delete your account, open **Settings → Delete account** in the app. You may need to sign in again before a sensitive deletion. A successful account deletion removes the account's linked server records and private cloud files. It does **not** automatically erase books and reading data already stored on your device, Android system backups, or the limited payment-event metadata described above.

For questions, access requests, or deletion help, write to **dp1.1a9.tung@gmail.com**. Tell us which NoCap account email is involved, but do not email passwords, bank credentials, or private book files. We may need to verify that you own the account before acting on a request.

## 9. Security, children, and policy updates

NoCap uses HTTPS for connections to its service, password and session-token hashing, and account-scoped authorization for private cloud data. No internet service can promise absolute security. NoCap is a general reading app and is not specifically designed for children. If you believe a child's personal information was provided in error, contact us using the address above.

We will update this page and its effective date when our data practices materially change. You can return to this URL to read the latest version.

---

# Chính sách quyền riêng tư của NoCap

**Ngày hiệu lực:** 24/09/2026

**Ứng dụng:** NoCap cho Android (`com.nocap.app`)

**Liên hệ về quyền riêng tư:** dp1.1a9.tung@gmail.com

Chính sách này mô tả cách NoCap xử lý thông tin khi bạn dùng ứng dụng Android và các dịch vụ tài khoản, đồng bộ, lưu trữ, thanh toán đi kèm.

## 1. NoCap hoạt động như thế nào

NoCap cho phép đọc sách và tài liệu, lưu tiến độ và ghi chú, đồng thời tùy chọn dùng tài khoản cho những tính năng cloud đủ điều kiện. Bạn có thể đọc cục bộ ở chế độ khách. Tài khoản cần thiết cho việc đồng bộ theo tài khoản, lưu trữ riêng trên cloud, sao lưu và quản lý thanh toán hoặc gói sử dụng.

## 2. Thông tin lưu trên thiết bị

Khi bạn nhập tệp hoặc mở tệp từ ứng dụng khác, NoCap đọc tệp bạn đã chọn và lưu một bản trong vùng dữ liệu ứng dụng để đọc. Dữ liệu cục bộ có thể gồm sách đã tải, tài liệu nhập, vị trí đọc, dấu trang, đoạn đánh dấu, ghi chú, tag, bộ sưu tập, mục ôn tập, phiên đọc và tùy chọn giao diện đọc. Việc chọn tệp hoặc dùng “Mở bằng” không cấp cho NoCap quyền đọc toàn bộ tệp trên thiết bị.

Dữ liệu đọc cục bộ không tự trở thành dữ liệu công khai. Dữ liệu có thể được gửi đến máy chủ NoCap khi bạn đăng nhập và dùng tính năng đồng bộ, lưu tài liệu riêng hoặc sao lưu cloud đủ điều kiện. Loại dữ liệu gửi đi phụ thuộc vào tính năng đó; thao tác nhập tệp thông thường không công bố tệp cho người dùng khác.

**Sao lưu của hệ điều hành Android khác với sao lưu cloud của NoCap.** Tùy cài đặt sao lưu và chuyển thiết bị, Android có thể sao chép một phần dữ liệu ứng dụng cục bộ vào tài khoản Google của bạn hoặc sang thiết bị khác. NoCap loại trừ tệp phiên đăng nhập và ảnh chụp trạng thái gói sử dụng khỏi cơ chế sao lưu này, nhưng dữ liệu cục bộ khác vẫn có thể được đưa vào. Bạn có thể quản lý sao lưu hệ thống trong phần cài đặt thiết bị hoặc tài khoản Google.

## 3. Thông tin được xử lý khi dùng tài khoản

- **Danh tính và hồ sơ:** Địa chỉ email, tên hiển thị, URL ảnh đại diện nếu có, trạng thái xác minh email, trạng thái tài khoản, thời điểm tạo tài khoản và đăng nhập gần đây. Nếu chọn đăng nhập Google, ứng dụng gửi mã định danh do Google cấp đến máy chủ NoCap để xác minh; máy chủ có thể giữ mã liên kết tài khoản Google. Nếu dùng email và mật khẩu, máy chủ lưu giá trị băm mật khẩu kèm muối thay vì mật khẩu nguyên văn.
- **Phiên đăng nhập và bảo mật:** NoCap cấp mã phiên cho ứng dụng và chỉ lưu giá trị băm của mã đó trên máy chủ. Phiên thường hết hạn trong tối đa 30 ngày và có thể bị thu hồi khi đăng xuất hoặc xóa tài khoản. Dịch vụ dùng bản ghi giới hạn tần suất tạm thời và thông tin yêu cầu, gồm địa chỉ IP được chuyển tới dịch vụ, để giảm lạm dụng. Nhà cung cấp hạ tầng cũng có thể xử lý nhật ký mạng và vận hành thông thường.
- **Thiết bị:** Để nhận diện thiết bị cho tài khoản và đồng bộ, dịch vụ lưu mã cài đặt, nền tảng, mẫu máy, phiên bản ứng dụng, thời điểm đăng ký và lần liên hệ gần nhất. Các chức năng đọc hiện tại không cần danh bạ, vị trí chính xác, micro hoặc camera.
- **Dữ liệu đọc và cloud:** Nếu tài khoản được dùng tính năng cloud và bạn sử dụng tính năng đó, NoCap có thể lưu tiến độ đọc, metadata thư viện, dấu trang, đoạn đánh dấu, ghi chú, tag, bộ sưu tập, mục ôn tập, phiên đọc, tệp tài liệu riêng và bản sao lưu cloud. Những dữ liệu này gắn với tài khoản để các thiết bị của bạn có thể truy xuất. Tệp riêng không được đưa vào danh mục sách công khai.
- **Gói sử dụng và quản trị:** Dịch vụ lưu gói Free/Pro, trạng thái và hạn sử dụng quyền lợi, cùng metadata liên quan đến giao dịch hoặc quyền thử nghiệm do quản trị viên cấp. Quản trị viên được ủy quyền có thể xem mã tài khoản, trạng thái gói, dung lượng tổng hợp và thông tin kỹ thuật về đồng bộ để vận hành dịch vụ. Dashboard được thiết kế để hiển thị metadata xung đột kỹ thuật thay vì nội dung tệp riêng hoặc ghi chú.

## 4. Thanh toán bằng chuyển khoản

Nếu bạn yêu cầu chuyển khoản để mua Pro, NoCap tạo mã thanh toán và lưu số tiền, đơn vị tiền, thời hạn gói, trạng thái, các mốc thời gian và tài khoản sở hữu đơn. SePay gửi thông báo giao dịch đã ký đến máy chủ. Để đối soát và ngăn xử lý trùng, NoCap lưu metadata giới hạn như mã giao dịch, số tiền, ngày, mã thanh toán, mã tham chiếu và giá trị băm của sự kiện nhận được. Máy chủ không lưu toàn bộ nội dung chuyển khoản, mật khẩu ngân hàng hoặc số thẻ thanh toán. Ứng dụng ngân hàng và SePay xử lý giao dịch theo chính sách riêng của họ. Google Play hiện chưa được cấu hình cho giao dịch thật trong bản Android hiện tại; chính sách này cần được rà soát nếu chức năng đó được bật sau này.

## 5. Mục đích sử dụng thông tin

Chúng tôi dùng thông tin để đăng nhập, xác minh email, đặt lại mật khẩu khi có yêu cầu, cung cấp chức năng đọc và cloud, đồng bộ dữ liệu đủ điều kiện giữa các thiết bị, quản lý lưu trữ và sao lưu, xác minh thanh toán và quyền sử dụng gói, ngăn lạm dụng, điều tra lỗi dịch vụ, và trả lời yêu cầu hỗ trợ hoặc quyền riêng tư. Ứng dụng Android hiện không tích hợp SDK quảng cáo hoặc SDK phân tích của bên thứ ba. Chúng tôi không bán dữ liệu cá nhân.

## 6. Khi thông tin được chuyển tới dịch vụ khác

- **Cloudflare** vận hành API, cơ sở dữ liệu và kho lưu trữ đối tượng riêng của NoCap; dịch vụ này có thể xử lý thông tin mạng hoặc vận hành cần thiết.
- **Google** xử lý thông tin khi bạn chọn đăng nhập Google. Cơ chế sao lưu hoặc chuyển thiết bị của Android có thể xử lý dữ liệu ứng dụng cục bộ nếu bạn bật; đây là dịch vụ khác với cloud của NoCap.
- **SePay và ngân hàng của bạn** xử lý giao dịch khi bạn chọn chuyển khoản. NoCap chỉ nhận thông tin giao dịch cần để đối soát đơn.
- **Dịch vụ email được cấu hình** gửi thư xác minh tài khoản và đặt lại mật khẩu đến địa chỉ email của bạn.
- **Nguồn sách và ảnh bìa bên ngoài** có thể nhận thông tin yêu cầu thông thường, như địa chỉ IP, khi tệp hoặc ảnh được tải từ máy chủ của họ.

Các nhà cung cấp này có thể xử lý thông tin ở quốc gia khác và áp dụng chính sách riêng. Chúng tôi không chủ ý công khai tài liệu, ghi chú hoặc bản sao lưu cloud riêng của bạn.

## 7. Thời gian lưu dữ liệu

- **Dữ liệu trên thiết bị:** Cho đến khi bạn xóa từng mục, xóa dữ liệu ứng dụng hoặc gỡ ứng dụng, tùy theo bản sao được giữ bởi cơ chế sao lưu/chuyển thiết bị Android.
- **Tài khoản và dữ liệu cloud:** Khi tài khoản còn tồn tại, hoặc cho đến khi bạn xóa từng mục hay bản sao lưu. Gói Pro hết hạn không tự xóa tệp và ghi chú cục bộ.
- **Phiên và dữ liệu bảo mật ngắn hạn:** Phiên hết hạn trong tối đa 30 ngày; bản ghi phiên đã hết hạn, tác vụ mật khẩu và giới hạn tần suất được dọn bằng tác vụ định kỳ của máy chủ. Đăng xuất xóa phiên đã lưu trên thiết bị và thử thu hồi phiên trên máy chủ. Nếu dịch vụ không kết nối được, phiên máy chủ có thể còn hiệu lực đến khi hết hạn.
- **Metadata thanh toán:** Đơn thanh toán gắn với tài khoản bị xóa cùng tài khoản. Metadata tối thiểu của sự kiện SePay có thể còn lại sau khi xóa tài khoản để đối soát, ngăn xử lý trùng, điều tra gian lận hoặc đáp ứng quy định pháp luật. Dịch vụ hiện chưa đặt thời hạn tự động xóa cho những bản ghi sự kiện này; bạn có thể liên hệ để yêu cầu xem xét xóa nếu được phép.
- **Bản sao ở bên thứ ba:** Google, ngân hàng, SePay, nguồn sách và nhà cung cấp hạ tầng có thể giữ thông tin theo chính sách riêng.

## 8. Lựa chọn và yêu cầu xóa

Bạn có thể đọc cục bộ ở chế độ khách, tự chọn có đăng nhập Google hay không, xóa từng sách cục bộ hoặc bản sao lưu cloud khi ứng dụng có thao tác tương ứng, và đăng xuất để xóa phiên đã lưu trên thiết bị. Để xóa tài khoản, vào **Cài đặt → Xóa tài khoản** trong ứng dụng. Bạn có thể cần đăng nhập lại trước thao tác nhạy cảm này. Khi xóa tài khoản thành công, bản ghi liên kết trên máy chủ và tệp riêng trên cloud sẽ bị xóa. Hành động này **không** tự xóa sách và dữ liệu đọc đã lưu trên thiết bị, bản sao lưu hệ thống Android hoặc metadata sự kiện thanh toán tối thiểu nêu trên.

Nếu có câu hỏi hoặc cần hỗ trợ truy cập/xóa dữ liệu, hãy gửi thư tới **dp1.1a9.tung@gmail.com**. Hãy cho biết email tài khoản NoCap liên quan, nhưng không gửi mật khẩu, thông tin đăng nhập ngân hàng hoặc tệp sách riêng qua email. Chúng tôi có thể cần xác minh bạn là chủ tài khoản trước khi xử lý yêu cầu.

## 9. Bảo mật, trẻ em và cập nhật chính sách

NoCap dùng HTTPS để kết nối với dịch vụ, băm mật khẩu và mã phiên, đồng thời kiểm tra quyền theo tài khoản khi truy cập dữ liệu cloud riêng. Không dịch vụ Internet nào bảo đảm an toàn tuyệt đối. NoCap là ứng dụng đọc nói chung, không được thiết kế riêng cho trẻ em. Nếu bạn tin rằng thông tin cá nhân của trẻ đã được cung cấp nhầm, hãy liên hệ qua địa chỉ ở trên.

Chúng tôi sẽ cập nhật trang này và ngày hiệu lực khi cách xử lý dữ liệu thay đổi đáng kể. Bạn có thể quay lại URL này để đọc phiên bản mới nhất.
