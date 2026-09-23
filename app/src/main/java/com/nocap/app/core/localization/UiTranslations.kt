package com.nocap.app.core.localization

internal object UiTranslations {
    private data class Entry(val vi: String, val en: String)

    private val entries = listOf(
        e("Ngang", "Horizontal"),
        e("Trang sau", "Next page"),
        e("vd: kinh-te, triet-hoc", "e.g. economics, philosophy"),
        e("Chưa mở được tài liệu. Hãy quay lại thư viện và thử lại; dữ liệu trên máy vẫn được giữ.", "Unable to open the document. Return to the library and try again; your local data is safe."),
        e("Tìm tên, tác giả, thẻ, bộ sưu tập...", "Search names, authors, tags, or collections…"),
        e("Đăng nhập để khôi phục giao dịch", "Sign in to restore purchases"),
        e("Hư cấu", "Fiction"),
        e("Phi hư cấu", "Non-fiction"),
        e("Trinh thám", "Mystery"),
        e("Khoa học viễn tưởng", "Science fiction"),
        e("Lãng mạn", "Romance"),
        e("Lịch sử", "History"),
        e("Tiểu sử", "Biography"),
        e("Phát triển bản thân", "Personal development"),
        e("Trang chủ", "Home"),
        e("Khám phá", "Discover"),
        e("Thư viện", "Library"),
        e("Bộ nhớ", "Memory"),
        e("Cài đặt", "Settings"),
        e("Tài khoản", "Account"),
        e("Ngôn ngữ", "Language"),
        e("Ngôn ngữ ứng dụng", "App language"),
        e("Thay đổi sẽ áp dụng ngay cho toàn bộ giao diện.", "Changes apply immediately across the app."),
        e("Tài liệu", "Documents"),
        e("Yêu thích", "Favorites"),
        e("Đọc tiếp", "Continue reading"),
        e("Tiếp tục đọc", "Continue reading"),
        e("Khám phá sách", "Discover books"),
        e("Khám phá thể loại", "Browse categories"),
        e("Sách nổi bật", "Featured books"),
        e("Sách mới", "New books"),
        e("Xem tất cả", "See all"),
        e("Mở danh mục sách", "Open book catalog"),
        e("Tìm sách, tác giả…", "Search books or authors…"),
        e("Tìm kiếm", "Search"),
        e("Sắp xếp", "Sort"),
        e("Tất cả", "All"),
        e("Mới", "New"),
        e("Đang đọc", "Reading"),
        e("Hoàn thành", "Completed"),
        e("Chưa đọc", "Not started"),
        e("Chưa phân loại", "Unsorted"),
        e("Thêm tài liệu", "Add document"),
        e("Thêm tài liệu mới", "Add a document"),
        e("Từ thiết bị", "From device"),
        e("Từ liên kết HTTPS", "From HTTPS link"),
        e("Nhập tài liệu từ liên kết", "Import from a link"),
        e("Nhập tài liệu", "Import document"),
        e("Đang xử lý tệp...", "Processing file…"),
        e("Đang xử lý tệp chia sẻ...", "Processing shared file…"),
        e("Nhập tài liệu thành công!", "Document imported successfully!"),
        e("Lỗi khi nhập tài liệu", "Document import failed"),
        e("Tài liệu đã tồn tại", "Document already exists"),
        e("Thử lại", "Try again"),
        e("Đóng", "Close"),
        e("Hủy", "Cancel"),
        e("Tiếp tục", "Continue"),
        e("Lưu", "Save"),
        e("Xóa", "Delete"),
        e("Sửa", "Edit"),
        e("Tạo", "Create"),
        e("Thêm", "Add"),
        e("Đổi tên", "Rename"),
        e("Quay lại", "Back"),
        e("Trở về", "Back"),
        e("Trước", "Previous"),
        e("Sau", "Next"),
        e("Tải về", "Download"),
        e("Đọc sách", "Read book"),
        e("Đọc tài liệu", "Read document"),
        e("Mở tài liệu", "Open document"),
        e("Không thể mở tài liệu", "Unable to open document"),
        e("Chưa mở được tài liệu", "Document could not be opened"),
        e("Chưa tải được tài liệu", "Document could not be downloaded"),
        e("Đang tải tài liệu...", "Downloading document…"),
        e("Chi tiết sách", "Book details"),
        e("Tác giả", "Author"),
        e("Tiến trình", "Progress"),
        e("Trạng thái", "Status"),
        e("Tùy chọn", "Options"),
        e("Ghim", "Pin"),
        e("Bỏ ghim", "Unpin"),
        e("Lưu trữ", "Archive"),
        e("Bỏ lưu trữ", "Unarchive"),
        e("Xóa khỏi thư viện", "Remove from library"),
        e("Sửa thông tin", "Edit details"),
        e("Đổi trạng thái đọc", "Change reading status"),
        e("Gắn thẻ", "Add tags"),
        e("Bộ sưu tập", "Collections"),
        e("Thẻ", "Tags"),
        e("Tạo bộ sưu tập", "Create collection"),
        e("Tạo thẻ", "Create tag"),
        e("Tên bộ sưu tập", "Collection name"),
        e("Tạo thẻ mới...", "Create a new tag…"),
        e("Dấu trang", "Bookmarks"),
        e("Tô sáng", "Highlights"),
        e("Ghi chú", "Notes"),
        e("Đánh dấu trang", "Bookmark"),
        e("Thêm dấu trang", "Add bookmark"),
        e("Xóa dấu trang", "Remove bookmark"),
        e("Thêm ghi chú & tô sáng", "Add note and highlight"),
        e("Tô sáng hoặc thêm ghi chú", "Highlight or add a note"),
        e("Nội dung ghi chú...", "Note…"),
        e("Thêm ghi chú (tùy chọn)...", "Add a note (optional)…"),
        e("Màu tô sáng:", "Highlight color:"),
        e("Mục lục", "Table of contents"),
        e("Tìm kiếm trong sách", "Search in book"),
        e("Tìm trong tài liệu...", "Search document…"),
        e("Cài đặt đọc tài liệu", "Reading settings"),
        e("Tùy chỉnh đọc", "Reading preferences"),
        e("Giao diện đọc sách", "Reading theme"),
        e("Chế độ màu", "Color theme"),
        e("Sáng", "Light"),
        e("Vàng giấy", "Sepia"),
        e("Tối", "Dark"),
        e("Cỡ chữ mặc định", "Default text size"),
        e("Kiểu chữ", "Typeface"),
        e("Mặc định", "Default"),
        e("Có chân", "Serif"),
        e("Không chân", "Sans serif"),
        e("Căn lề văn bản", "Text alignment"),
        e("Trái", "Left"),
        e("Đều", "Justified"),
        e("Cuộn dọc", "Vertical scrolling"),
        e("Chế độ cuộn liên tục", "Continuous scrolling"),
        e("Điều khiển & Màn hình", "Controls and display"),
        e("Chế độ toàn màn hình", "Full screen"),
        e("Giữ màn hình luôn sáng", "Keep screen awake"),
        e("Phím âm lượng lật trang", "Turn pages with volume keys"),
        e("Thông tin đọc sách ở chân trang", "Reading info in footer"),
        e("Theo hệ thống", "System default"),
        e("Khôi phục cài đặt đọc mặc định", "Restore default reading settings"),
        e("Quản lý cài đặt", "Manage settings"),
        e("Thông tin ứng dụng", "App information"),
        e("Tên ứng dụng", "App name"),
        e("Phiên bản", "Version"),
        e("Bộ đọc sách", "Reader engine"),
        e("Tùy chỉnh đọc sách", "Reading preferences"),
        e("Đăng nhập", "Sign in"),
        e("Đăng xuất", "Sign out"),
        e("Tạo tài khoản", "Create account"),
        e("Tạo tài khoản mới", "Create a new account"),
        e("Đăng ký tài khoản", "Create account"),
        e("Email", "Email"),
        e("Mật khẩu", "Password"),
        e("Mật khẩu (tối thiểu 12 ký tự)", "Password (at least 12 characters)"),
        e("Xác nhận mật khẩu", "Confirm password"),
        e("Quên mật khẩu?", "Forgot password?"),
        e("Quên mật khẩu", "Reset password"),
        e("Đăng nhập với Google", "Sign in with Google"),
        e("Tiếp tục với tư cách khách", "Continue as guest"),
        e("Bạn đang dùng chế độ khách", "You are using Guest mode"),
        e("Đổi tên hiển thị", "Change display name"),
        e("Tên hiển thị mới", "New display name"),
        e("Xóa tài khoản", "Delete account"),
        e("Xác nhận xóa", "Confirm deletion"),
        e("Đã xác thực", "Verified"),
        e("Gửi lại link", "Resend link"),
        e("Đồng bộ nhiều thiết bị", "Multi-device sync"),
        e("Đồng bộ ngay", "Sync now"),
        e("Đã đồng bộ", "Synced"),
        e("Đang đồng bộ", "Syncing"),
        e("Không có mạng", "Offline"),
        e("Bản sao lưu thư viện", "Library backup"),
        e("Sao lưu", "Back up"),
        e("Khôi phục", "Restore"),
        e("Xóa bản sao lưu", "Delete backup"),
        e("Nâng cấp Pro", "Upgrade to Pro"),
        e("Nâng cấp Pro qua ngân hàng", "Upgrade to Pro by bank transfer"),
        e("Đăng nhập để nâng cấp Pro.", "Sign in to upgrade to Pro."),
        e("Thanh toán chuyển khoản chưa sẵn sàng. Vui lòng thử lại sau.", "Bank transfer payments are not ready yet. Please try again later."),
        e("Chưa tải được danh sách ứng dụng ngân hàng. Bạn vẫn có thể sao chép thông tin để chuyển khoản.", "The bank app list could not be loaded. You can still copy the transfer details."),
        e("Chưa tạo được yêu cầu thanh toán. Vui lòng kiểm tra mạng và thử lại.", "The payment request could not be created. Check your connection and try again."),
        e("Thanh toán thành công. Gói Pro đã được kích hoạt.", "Payment successful. Pro is now active."),
        e("Đã sao chép số tài khoản.", "Account number copied."),
        e("Đã sao chép nội dung chuyển khoản.", "Transfer memo copied."),
        e("Chưa kiểm tra được giao dịch. Vui lòng thử lại.", "The payment status could not be checked. Please try again."),
        e("Không mở được ứng dụng ngân hàng. Nội dung chuyển khoản đã được sao chép.", "The bank app could not be opened. The transfer memo has been copied."),
        e("Thanh toán thành công", "Payment successful"),
        e("Thanh toán nâng cấp Pro", "Pro upgrade payment"),
        e("Mã QR thanh toán Pro", "Pro payment QR code"),
        e("Quét mã QR để thanh toán", "Scan the QR code to pay"),
        e("Gói Pro đã được kích hoạt cho tài khoản này.", "Pro has been activated for this account."),
        e("Quét QR hoặc dùng thông tin bên dưới. Chuyển đúng số tiền và nội dung để hệ thống tự xác nhận.", "Scan the QR code or use the details below. Transfer the exact amount and memo for automatic confirmation."),
        e("Số tiền", "Amount"),
        e("Ngân hàng", "Bank"),
        e("Chủ tài khoản", "Account holder"),
        e("Số tài khoản", "Account number"),
        e("Nội dung chuyển khoản", "Transfer memo"),
        e("Mở ứng dụng ngân hàng", "Open bank app"),
        e("Kiểm tra thanh toán", "Check payment"),
        e("Chọn ứng dụng ngân hàng", "Choose a bank app"),
        e("Tự điền", "Auto-fill"),
        e("Khôi phục giao dịch", "Restore purchases"),
        e("Thanh toán đang chờ xác nhận", "Purchase pending"),
        e("Cập nhật trạng thái gói", "Refresh plan status"),
        e("Bộ nhớ đọc", "Reading Memory"),
        e("Trở lại những điều bạn muốn nhớ", "Return to what you want to remember"),
        e("Ôn tập", "Review"),
        e("Ôn tập hàng ngày", "Daily review"),
        e("Ôn tập ngắt quãng", "Spaced review"),
        e("Tìm kiếm tri thức", "Knowledge Search"),
        e("Thống kê đọc", "Reading statistics"),
        e("Toàn bộ Ghi chú & Đoạn trích", "All notes and highlights"),
        e("Ghi chú & Đoạn trích gần đây", "Recent notes and highlights"),
        e("Không tìm thấy kết quả nào", "No results found"),
        e("Không có nội dung nào phù hợp", "No matching content"),
        e("Hôm nay", "Today"),
        e("7 ngày qua", "Past 7 days"),
        e("30 ngày qua", "Past 30 days"),
        e("Tổng phiên đọc", "Total reading sessions"),
        e("Chưa nhớ", "Again"),
        e("Khó", "Hard"),
        e("Tốt", "Good"),
        e("Dễ", "Easy"),
        e("Chia sẻ", "Share"),
        e("Sao chép", "Copy"),
        e("Xuất Markdown", "Export Markdown"),
        e("Đã hiểu", "Got it"),
        e("Không thể mở sách", "Unable to open book"),
        e("Không tìm thấy sách", "Book not found"),
        e("Không tìm thấy tài liệu", "Document not found"),
        e("Đã thêm phông chữ: ", "Font added: "),
        e("Không thể thêm phông chữ", "Unable to add font"),
        e("Đã sao chép trích dẫn", "Quote copied"),
        e("Đã xuất dữ liệu ghi chú thành công!", "Notes exported successfully!"),
        e("Lỗi khi xuất tệp Markdown", "Unable to export Markdown"),
        e("Không thể mở tệp để ghi", "Unable to open the destination file"),
        e("Bạn đang dùng chế độ khách", "You are using Guest mode"),
        e("Đăng nhập để sao lưu và khôi phục thư viện của bạn", "Sign in to back up and restore your library"),
        e("Yêu cầu xác thực Email", "Email verification required"),
        e("Email đã xác thực", "Email verified"),
        e("Máy chủ ngoại tuyến. Tính năng ngoại tuyến vẫn hoạt động bình thường.", "Server unavailable. Offline features still work normally."),
        e("Đăng xuất chuyển sang thư viện Khách riêng biệt. Dữ liệu tài khoản vẫn được giữ để dùng tiếp khi đăng nhập lại.", "Signing out switches to a separate Guest library. Your account data remains available when you sign in again."),
        e("Sao lưu và khôi phục sách, ghi chú, tiến độ, thẻ và bộ sưu tập.", "Back up and restore books, notes, progress, tags, and collections."),
        e("Sao lưu thư viện?", "Back up library?"),
        e("Khôi phục thư viện?", "Restore library?"),
        e("Xóa bản sao lưu?", "Delete backup?"),
        e("Bản sao lưu trên đám mây sẽ bị xóa. Dữ liệu trong máy vẫn được giữ.", "The cloud backup will be deleted. Data on this device will remain."),
        e("Thư viện hiện tại sẽ được thay bằng bản sao lưu trên tài khoản này. Hãy sao lưu dữ liệu cần giữ trước khi tiếp tục.", "The current library will be replaced by this account's backup. Back up anything you want to keep before continuing."),
        e("Sách, ghi chú, tiến độ và bộ sưu tập trên máy sẽ được tải lên tài khoản đang đăng nhập. Bản sao lưu gần nhất sẽ được thay thế.", "Books, notes, progress, and collections on this device will be uploaded to the signed-in account, replacing its latest backup."),
        e("Free luôn giữ quyền đọc, nhập tài liệu, dấu trang, ghi chú và thư viện offline. Pro thêm đồng bộ và lưu trữ đám mây.", "Free always includes reading, document import, bookmarks, notes, and an offline library. Pro adds sync and cloud storage."),
        e("Pro chưa mở bán. Bạn vẫn có thể đọc và quản lý tài liệu trên máy với Free.", "Pro is not available for purchase yet. You can continue reading and managing local documents with Free."),
        e("Pro không hoạt động. Tài liệu và thay đổi chưa đồng bộ vẫn được giữ trên máy.", "Pro is inactive. Local documents and unsynced changes remain on this device."),
        e("Khôi phục cài đặt?", "Restore settings?"),
        e("Bạn có chắc chắn muốn đặt lại toàn bộ tùy chỉnh giao diện và phông chữ đọc sách về mặc định?", "Reset all reading appearance and font preferences to their defaults?"),
        e("Đặt lại", "Reset"),
        e("Đặt lại cỡ chữ 100%, giao diện sáng và kiểu chữ mặc định", "Reset to 100% text size, light theme, and default typeface"),
        e("Xác nhận xóa tài khoản?", "Delete account?"),
        e("Hành động này sẽ xóa vĩnh viễn tài khoản của bạn và toàn bộ dữ liệu hồ sơ liên kết trên máy chủ. Sách đã tải về trên máy và tiến trình đọc cục bộ sẽ được giữ lại.", "This permanently deletes your account and linked profile data from the server. Downloaded books and local reading progress remain on this device."),
        e("Thư viện tài liệu của bạn đang trống", "Your document library is empty"),
        e("Nhập tài liệu EPUB hoặc PDF từ thiết bị hoặc tải từ liên kết để bắt đầu.", "Import an EPUB or PDF from your device, or download one from a link to get started."),
        e("Bộ lọc hiện tại chưa có kết quả. Chọn Tất cả ở phía trên để xem lại thư viện.", "No results match the current filter. Select All above to view your library."),
        e("Không có tài liệu chưa phân loại", "No unsorted documents"),
        e("Mở Tất cả để xem thư viện, hoặc thêm tài liệu mới bằng nút phía dưới.", "Open All to view your library, or add a document with the button below."),
        e("Chưa có bộ sưu tập nào", "No collections yet"),
        e("Chưa có bộ sưu tập nào.", "No collections yet."),
        e("Chưa có thẻ nào", "No tags yet"),
        e("Chưa có sách để giới thiệu", "No books to recommend yet"),
        e("Sách được đánh dấu yêu thích sẽ xuất hiện ở đây", "Books you favorite will appear here"),
        e("Sách yêu thích", "Favorite books"),
        e("Chỉ chấp nhận liên kết HTTPS an toàn", "Only secure HTTPS links are accepted"),
        e("Nhập liên kết HTTPS tới tệp tài liệu hoặc trang bài viết. Với trang web, app chỉ giữ nội dung chính và loại bỏ liên kết, menu, quảng cáo.", "Enter an HTTPS link to a document or article. For web pages, the app keeps the main content and removes links, menus, and ads."),
        e("Chọn tệp EPUB hoặc PDF có sẵn trên máy", "Choose an EPUB or PDF on this device"),
        e("Tải tài liệu trực tiếp từ liên kết mạng an toàn", "Download a document from a secure web link"),
        e("Cuốn sách này không có mục lục", "This book has no table of contents"),
        e("Không có văn bản để chọn", "No text available to select"),
        e("Chưa có dấu trang nào.\nNhấn biểu tượng dấu trang trên thanh tiêu đề để lưu vị trí đọc.", "No bookmarks yet.\nTap the bookmark icon in the top bar to save your reading position."),
        e("Chưa có đoạn văn nào được tô sáng.\nChọn đoạn văn trong sách để tô sáng hoặc ghi chú.", "No highlights yet.\nSelect text in the book to highlight it or add a note."),
        e("Chưa có ghi chú nào.\nChọn đoạn văn và chọn Ghi chú để lưu ý tưởng của bạn.", "No notes yet.\nSelect text and choose Note to save your thoughts."),
        e("Tính năng tìm kiếm toàn văn hiện tại chỉ hỗ trợ định dạng EPUB.\nĐịnh dạng tài liệu hiện tại không hỗ trợ bộ chỉ mục tìm kiếm.", "Full-text search currently supports EPUB only.\nThis document format does not support a search index."),
        e("Tìm kiếm trong toàn bộ tri thức của bạn", "Search across your knowledge"),
        e("Tìm tri thức, tài liệu, ghi chú...", "Search knowledge, documents, notes…"),
        e("Tìm tài liệu, đoạn trích, ghi chú, đánh dấu trang hoặc nội dung", "Search documents, highlights, notes, bookmarks, or content"),
        e("Tìm đoạn tô sáng, ghi chú hoặc ôn lại những ý đã lưu khi đọc.", "Find highlights and notes, or review ideas you saved while reading."),
        e("Chưa có ghi chú hoặc đoạn trích nào.\nHãy chọn văn bản khi đọc để lưu giữ tri thức!", "No notes or highlights yet.\nSelect text while reading to save useful ideas."),
        e("Bạn đã hoàn thành tất cả mục ôn tập hôm nay!", "You completed every review for today!"),
        e("Đánh giá mức độ nhớ của bạn:", "How well did you remember?"),
        e("Xem trong tài liệu gốc", "View in source document"),
        e("Thống kê & Thói quen đọc", "Reading statistics and habits"),
        e("Tổng quan thời gian đọc", "Reading time overview"),
        e("Tài liệu đọc nhiều nhất", "Most-read documents"),
        e("Không có kết quả phù hợp", "No matching results"),
        e("Tìm kiếm tri thức trong toàn bộ thư viện...", "Search knowledge across your library…"),
        e("Đã đọc xong", "Finished reading"),
        e("Ghim lên đầu", "Pin to top"),
        e("Đã phân loại", "Mark as organized"),
        e("Hộp thư đến", "Move to inbox"),
        e("Xóa tài liệu", "Delete document"),
        e("Mới đọc", "Recently read"),
        e("Đã ghim", "Pinned"),
        e("Tiến độ đọc", "Reading progress"),
        e("Mới thêm", "Recently added"),
        e("Tên (A-Z)", "Title (A–Z)"),
        e("Tên (Z-A)", "Title (Z–A)"),
        e("Hình ảnh", "Images"),
        e("Từ Web", "From the web"),
        e("Chưa tải được tài liệu. Kiểm tra kết nối và dung lượng trống rồi thử lại. Sách đã lưu vẫn được giữ.", "Could not download. Check your connection and free storage, then try again. Saved books are safe."),
        e("Ghi nhớ", "Memory"),
        e("Đồng bộ & Sao lưu", "Sync & Backup"),
        e("Gói dịch vụ", "Subscription plan"),
        e("Trải nghiệm đọc", "Reading experience"),
        e("Thông tin ứng dụng", "App information"),
        e("Đã xác nhận gói thành công", "Plan verified successfully"),
        e("Đã kiểm tra gói; một số giao dịch chưa khôi phục được hoặc thuộc tài khoản khác.", "Plan checked; some purchases could not be restored or belong to another account."),
        e("Chọn tệp EPUB, PDF, TXT, MD, HTML, DOCX, CBZ hoặc ảnh", "Choose EPUB, PDF, TXT, MD, HTML, DOCX, CBZ, or image files"),
        e("Thêm tài liệu từ thiết bị hoặc tải từ liên kết để bắt đầu (hỗ trợ EPUB, PDF, TXT, MD, HTML, DOCX, CBZ, ảnh).", "Add documents from your device or download from a link (supports EPUB, PDF, TXT, MD, HTML, DOCX, CBZ, images)."),
        e("Không gian đọc", "Reading Workspace"),
        e("Tài liệu gần đây", "Recent documents"),
        e("Ghi nhớ gần đây", "Recent notes & highlights"),
        e("Khám phá thêm", "Explore more"),
        e("Tiếp tục hành trình đọc và tích lũy tri thức", "Continue your reading and knowledge journey"),
        e("Dữ liệu trên máy được lưu riêng theo tài khoản. Đăng xuất sẽ chuyển về thư viện Khách.", "Local data is isolated per account. Signing out switches to the Guest library."),
        e("Đăng nhập để đồng bộ và sao lưu thư viện giữa các thiết bị", "Sign in to sync and back up your library across devices"),
        e("Đăng nhập tài khoản để tự động đồng bộ tiến độ đọc và sao lưu thư viện giữa các thiết bị.", "Sign in to automatically sync reading progress and back up your library across devices."),
        e("Sao lưu & Khôi phục", "Backup & Restore"),
        e("Lưu trữ toàn bộ sách, ghi chú, tiến độ đọc và bộ sưu tập lên tài khoản.", "Save all books, notes, reading progress, and collections to your account."),
        e("Xóa bản sao lưu trên đám mây", "Delete cloud backup"),
        e("Gói hiện tại: Pro", "Current plan: Pro"),
        e("Gói hiện tại: Free", "Current plan: Free"),
        e("Làm mới trạng thái gói", "Refresh plan status"),
        e("Tiếp tục đọc", "Continue reading"),
        e("Sách tuyển chọn", "Featured books"),
        e("Khám phá theo thể loại", "Explore by category"),
        e("Chào mừng bạn đến với Không gian đọc", "Welcome to your Reading Workspace"),
        e("Thêm sách hoặc tài liệu từ thiết bị để bắt đầu trải nghiệm đọc cá nhân.", "Add books or documents from your device to start your personal reading experience."),
        e("Mở Thư viện tài liệu", "Open Document Library")
    )

    private val byVietnamese = entries.associateBy(Entry::vi)

    fun translate(source: String, language: AppLanguage): String {
        if (language == AppLanguage.VIETNAMESE) return source
        byVietnamese[source]?.let { return it.forLanguage(language) }
        translateTemplate(source, language)?.let { return it }
        EnglishFallbacks[source]?.let { return it }
        return source
    }

    private fun translateTemplate(source: String, language: AppLanguage): String? {
        fun match(pattern: String) = Regex(pattern).matchEntire(source)
        match("(\\d+) tài liệu")?.let { m ->
            val n = m.groupValues[1]
            return "$n documents"
        }
        match("Trang (\\d+)")?.let { m ->
            val n = m.groupValues[1]
            return "Page $n"
        }
        match("Tìm thấy (\\d+) kết quả")?.let { m ->
            val n = m.groupValues[1]
            return "$n results found"
        }
        match("(\\d+) kết quả cho \"(.+)\"")?.let { m ->
            val count = m.groupValues[1]
            val query = m.groupValues[2]
            return "$count results for “$query”"
        }
        match("(\\d+) phiên")?.let { m ->
            val count = m.groupValues[1]
            return "$count sessions"
        }
        match("(\\d+) nội dung cần ôn tập hôm nay")?.let { m ->
            val count = m.groupValues[1]
            return "$count items to review today"
        }
        match("Bắt đầu ôn tập \\((\\d+)\\)")?.let { m ->
            val count = m.groupValues[1]
            return "Start review ($count)"
        }
        match("Thẻ (\\d+) / (\\d+)")?.let { m ->
            return "Card ${m.groupValues[1]} / ${m.groupValues[2]}"
        }
        match("Đang tải: (.+)")?.let { m ->
            return "Downloading: ${m.groupValues[1]}"
        }
        match("Cập nhật \\(phiên bản (.+)\\)")?.let { m ->
            return "Update (version ${m.groupValues[1]})"
        }
        match("Yêu cầu hết hạn: (.+)")?.let { m ->
            return "Request expires: ${m.groupValues[1]}"
        }
        match("Chọn bộ sưu tập cho (\\d+) tài liệu:")?.let { m ->
            return "Choose collections for ${m.groupValues[1]} documents:"
        }
        match("Chọn thẻ để gắn cho (\\d+) tài liệu đã chọn:")?.let { m ->
            return "Choose tags for ${m.groupValues[1]} selected documents:"
        }
        match("Chọn (?:các bộ sưu tập|thẻ) cho: \"(.+)\"")?.let { m ->
            return "Choose organization for “${m.groupValues[1]}”"
        }
        match("Chưa có tài liệu trong (.+)")?.let { m ->
            return "No documents in ${m.groupValues[1]}"
        }
        match("Chưa phân loại \\((\\d+)\\)")?.let { m ->
            return "Unsorted (${m.groupValues[1]})"
        }
        match("Đã (bỏ ghim|bỏ lưu trữ|gắn thẻ cho|ghim|lưu trữ) (\\d+) tài liệu")?.let { m ->
            val action = when (m.groupValues[1]) {
                "bỏ ghim" -> "unpinned"
                "bỏ lưu trữ" -> "unarchived"
                "gắn thẻ cho" -> "tagged"
                "ghim" -> "pinned"
                else -> "archived"
            }
            return "${m.groupValues[2]} documents $action"
        }
        match("Đã thêm (\\d+) tài liệu vào bộ sưu tập")?.let { m ->
            return "Added ${m.groupValues[1]} documents to collections"
        }
        match("Đã (?:cập nhật trạng thái|đánh dấu): (.+)")?.let { m ->
            val status = translate(m.groupValues[1], language)
            return "Status updated: $status"
        }
        match("Đã chọn: (\\d+)")?.let { m ->
            return "Selected: ${m.groupValues[1]}"
        }
        match("Đã đổi tên thành: (.+)")?.let { m ->
            return "Renamed to: ${m.groupValues[1]}"
        }
        match("Đã tạo (bộ sưu tập|thẻ): (.+)")?.let { m ->
            return "Created ${m.groupValues[2]}"
        }
        match("Gốc: (.+)")?.let { m ->
            return "Original: ${m.groupValues[1]}"
        }
        match("Kiểm tra hộp thư (.+) hoặc nhấn Gửi lại link\\.")?.let { m ->
            return "Check ${m.groupValues[1]} or tap Resend link."
        }
        match("Lần ôn: (\\d+)")?.let { m ->
            return "Reviews: ${m.groupValues[1]}"
        }
        match("Tất cả (\\d+) điểm trích dẫn đã có trong danh sách ôn tập\\.")?.let { m ->
            return "All ${m.groupValues[1]} highlights are already in the review queue."
        }
        match("Ôn tập tất cả thẻ \\((\\d+)\\)")?.let { m ->
            return "Review all cards (${m.groupValues[1]})"
        }
        match("Tất cả \\((\\d+)\\)")?.let { m ->
            return "All (${m.groupValues[1]})"
        }
        match("Cần ôn lại \\((\\d+)\\)")?.let { m ->
            return "Due (${m.groupValues[1]})"
        }
        match("Đang học \\((\\d+)\\)")?.let { m ->
            return "Learning (${m.groupValues[1]})"
        }
        match("Đã nhớ \\((\\d+)\\)")?.let { m ->
            return "Mastered (${m.groupValues[1]})"
        }
        match("(\\d+) / (\\d+) thẻ")?.let { m ->
            return "${m.groupValues[1]} / ${m.groupValues[2]} cards"
        }
        match("Đã thêm (\\d+) thẻ mới vào danh sách ôn tập!")?.let { m ->
            return "Added ${m.groupValues[1]} new cards to the review queue!"
        }
        match("Bạn đã ôn luyện xong (\\d+) thẻ trong phiên này\\.")?.let { m ->
            return "You reviewed ${m.groupValues[1]} cards in this session."
        }
        match("Lịch sử sao lưu \\((\\d+)\\)")?.let { m ->
            return "Backup history (${m.groupValues[1]})"
        }
        match("(.+) • Thiết bị")?.let { m ->
            return "${m.groupValues[1]} • Device"
        }
        match("Không thể tạo danh sách ôn tập: (.+)")?.let { m ->
            return "Unable to create the review queue: ${m.groupValues[1]}"
        }
        match("Không thể tải trang (\\d+)\\. Tệp ảnh có thể bị hỏng hoặc không được hỗ trợ\\.")?.let { m ->
            return "Unable to load page ${m.groupValues[1]}. The image may be damaged or unsupported."
        }
        match("Không thể hiển thị trang (\\d+)\\. Định dạng ảnh có thể bị hỏng hoặc không được hỗ trợ\\.")?.let { m ->
            return "Unable to display page ${m.groupValues[1]}. The image format may be damaged or unsupported."
        }
        match("Dữ liệu hiện tại sẽ được thay thế bằng bản sao lưu lúc (.+) \\((.+) MB\\)\\. Thao tác này sẽ ghi đè thư viện hiện tại\\.")?.let { m ->
            return "The current library will be replaced by the backup from ${m.groupValues[1]} (${m.groupValues[2]} MB). This overwrites the current library."
        }
        match("Bản sao lưu lúc (.+) \\((.+) MB\\) sẽ bị xóa vĩnh viễn khỏi đám mây\\.")?.let { m ->
            return "The backup from ${m.groupValues[1]} (${m.groupValues[2]} MB) will be permanently deleted from the cloud."
        }
        match("Bản sao lưu (.+)")?.let { m ->
            return "Backup ${m.groupValues[1]}"
        }
        match("(?:Lỗi lưu ảnh bìa|Lỗi xác thực|Máy chủ backend chưa khả dụng|Tải thất bại): (.+)")?.let { m ->
            return "Error: ${m.groupValues[1]}"
        }
        match("Tài liệu: \"(.+)\" đã có trong thư viện của bạn\\.")?.let { m ->
            return "“${m.groupValues[1]}” is already in your library."
        }
        match("Tài liệu: \"(.+)\" và toàn bộ dấu trang, ghi chú, tiến độ đọc sẽ bị xóa vĩnh viễn khỏi thiết bị\\.")?.let { m ->
            return "“${m.groupValues[1]}” and its bookmarks, notes, and reading progress will be permanently removed from this device."
        }
        match("Trích đoạn & Ghi chú \\(Trang (\\d+)\\)")?.let { m ->
            return "Highlights and notes (Page ${m.groupValues[1]})"
        }
        match("Trình đọc văn bản không hỗ trợ định dạng (.+)")?.let { m ->
            return "The text reader does not support ${m.groupValues[1]}"
        }
        match("Xóa thẻ: #(.+)\\?")?.let { m ->
            return "Delete tag #${m.groupValues[1]}?"
        }
        match("Đã thêm \"(.+)\" vào thư viện!")?.let { m ->
            val title = m.groupValues[1]
            return "Added “$title” to your library!"
        }
        match("(Dấu trang|Tô sáng|Ghi chú|Bộ sưu tập|Thẻ) \\((\\d+)\\)")?.let { m ->
            val label = byVietnamese[m.groupValues[1]]?.forLanguage(language) ?: m.groupValues[1]
            return "$label (${m.groupValues[2]})"
        }
        match("Cỡ chữ: (.+)")?.let { m ->
            return "Text size: ${m.groupValues[1]}"
        }
        match("Giãn dòng: (.+)")?.let { m ->
            return "Line spacing: ${m.groupValues[1]}"
        }
        match("Kết quả (.+)")?.let { m ->
            return "Result ${m.groupValues[1]}"
        }
        return null
    }

    private fun Entry.forLanguage(language: AppLanguage) = when (language) {
        AppLanguage.ENGLISH -> en
        AppLanguage.VIETNAMESE -> vi
    }

    private fun e(vi: String, en: String) = Entry(vi, en)
}
