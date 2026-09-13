package com.nocap.app.core.localization

internal object UiTranslations {
    private data class Entry(val vi: String, val en: String, val ja: String, val zh: String, val ko: String)

    private val entries = listOf(
        e("Tìm tên, tác giả, thẻ, bộ sưu tập...", "Search names, authors, tags, or collections…", "タイトル、著者、タグ、コレクションを検索…", "搜索标题、作者、标签或合集…", "제목, 저자, 태그, 컬렉션 검색…"),
        e("Đăng nhập để khôi phục giao dịch", "Sign in to restore purchases", "購入を復元するにはログインしてください", "登录以恢复购买", "구매를 복원하려면 로그인하세요"),
        e("Hư cấu", "Fiction", "フィクション", "虚构", "소설"),
        e("Phi hư cấu", "Non-fiction", "ノンフィクション", "非虚构", "논픽션"),
        e("Trinh thám", "Mystery", "ミステリー", "推理", "추리"),
        e("Khoa học viễn tưởng", "Science fiction", "SF", "科幻", "공상 과학"),
        e("Lãng mạn", "Romance", "ロマンス", "爱情", "로맨스"),
        e("Lịch sử", "History", "歴史", "历史", "역사"),
        e("Tiểu sử", "Biography", "伝記", "传记", "전기"),
        e("Phát triển bản thân", "Personal development", "自己啓発", "个人成长", "자기 계발"),
        e("Trang chủ", "Home", "ホーム", "首页", "홈"),
        e("Khám phá", "Discover", "見つける", "发现", "둘러보기"),
        e("Thư viện", "Library", "ライブラリ", "书库", "라이브러리"),
        e("Bộ nhớ", "Memory", "メモリー", "记忆", "메모리"),
        e("Cài đặt", "Settings", "設定", "设置", "설정"),
        e("Tài khoản", "Account", "アカウント", "账户", "계정"),
        e("Ngôn ngữ", "Language", "言語", "语言", "언어"),
        e("Ngôn ngữ ứng dụng", "App language", "アプリの言語", "应用语言", "앱 언어"),
        e("Thay đổi sẽ áp dụng ngay cho toàn bộ giao diện.", "Changes apply immediately across the app.", "変更はアプリ全体にすぐ反映されます。", "更改会立即应用到整个应用。", "변경 사항은 앱 전체에 즉시 적용됩니다."),
        e("Tài liệu", "Documents", "ドキュメント", "文档", "문서"),
        e("Yêu thích", "Favorites", "お気に入り", "收藏", "즐겨찾기"),
        e("Đọc tiếp", "Continue reading", "続きを読む", "继续阅读", "계속 읽기"),
        e("Tiếp tục đọc", "Continue reading", "続きを読む", "继续阅读", "계속 읽기"),
        e("Khám phá sách", "Discover books", "本を見つける", "发现图书", "도서 둘러보기"),
        e("Khám phá thể loại", "Browse categories", "カテゴリーを探す", "浏览分类", "카테고리 둘러보기"),
        e("Sách nổi bật", "Featured books", "注目の本", "精选图书", "추천 도서"),
        e("Sách mới", "New books", "新着", "新书", "신간"),
        e("Xem tất cả", "See all", "すべて表示", "查看全部", "모두 보기"),
        e("Mở danh mục sách", "Open book catalog", "ブックカタログを開く", "打开图书目录", "도서 카탈로그 열기"),
        e("Tìm sách, tác giả…", "Search books or authors…", "本や著者を検索…", "搜索图书或作者…", "도서 또는 저자 검색…"),
        e("Tìm kiếm", "Search", "検索", "搜索", "검색"),
        e("Sắp xếp", "Sort", "並べ替え", "排序", "정렬"),
        e("Tất cả", "All", "すべて", "全部", "전체"),
        e("Mới", "New", "新着", "最新", "신규"),
        e("Đang đọc", "Reading", "読書中", "阅读中", "읽는 중"),
        e("Hoàn thành", "Completed", "完了", "已完成", "완료"),
        e("Chưa đọc", "Not started", "未読", "未读", "읽지 않음"),
        e("Chưa phân loại", "Unsorted", "未分類", "未分类", "미분류"),
        e("Thêm tài liệu", "Add document", "ドキュメントを追加", "添加文档", "문서 추가"),
        e("Thêm tài liệu mới", "Add a document", "ドキュメントを追加", "添加文档", "문서 추가"),
        e("Từ thiết bị", "From device", "デバイスから", "从设备", "기기에서"),
        e("Từ liên kết HTTPS", "From HTTPS link", "HTTPSリンクから", "从 HTTPS 链接", "HTTPS 링크에서"),
        e("Nhập tài liệu từ liên kết", "Import from a link", "リンクからインポート", "从链接导入", "링크에서 가져오기"),
        e("Nhập tài liệu", "Import document", "ドキュメントをインポート", "导入文档", "문서 가져오기"),
        e("Đang xử lý tệp...", "Processing file…", "ファイルを処理中…", "正在处理文件…", "파일 처리 중…"),
        e("Đang xử lý tệp chia sẻ...", "Processing shared file…", "共有ファイルを処理中…", "正在处理共享文件…", "공유 파일 처리 중…"),
        e("Nhập tài liệu thành công!", "Document imported successfully!", "ドキュメントをインポートしました。", "文档导入成功！", "문서를 가져왔습니다!"),
        e("Lỗi khi nhập tài liệu", "Document import failed", "ドキュメントをインポートできませんでした", "文档导入失败", "문서를 가져오지 못했습니다"),
        e("Tài liệu đã tồn tại", "Document already exists", "ドキュメントは既に存在します", "文档已存在", "문서가 이미 있습니다"),
        e("Thử lại", "Try again", "再試行", "重试", "다시 시도"),
        e("Đóng", "Close", "閉じる", "关闭", "닫기"),
        e("Hủy", "Cancel", "キャンセル", "取消", "취소"),
        e("Tiếp tục", "Continue", "続ける", "继续", "계속"),
        e("Lưu", "Save", "保存", "保存", "저장"),
        e("Xóa", "Delete", "削除", "删除", "삭제"),
        e("Sửa", "Edit", "編集", "编辑", "편집"),
        e("Tạo", "Create", "作成", "创建", "만들기"),
        e("Thêm", "Add", "追加", "添加", "추가"),
        e("Đổi tên", "Rename", "名前を変更", "重命名", "이름 변경"),
        e("Quay lại", "Back", "戻る", "返回", "뒤로"),
        e("Trở về", "Back", "戻る", "返回", "뒤로"),
        e("Trước", "Previous", "前へ", "上一个", "이전"),
        e("Sau", "Next", "次へ", "下一个", "다음"),
        e("Tải về", "Download", "ダウンロード", "下载", "다운로드"),
        e("Đọc sách", "Read book", "読む", "阅读", "읽기"),
        e("Đọc tài liệu", "Read document", "ドキュメントを読む", "阅读文档", "문서 읽기"),
        e("Mở tài liệu", "Open document", "ドキュメントを開く", "打开文档", "문서 열기"),
        e("Không thể mở tài liệu", "Unable to open document", "ドキュメントを開けません", "无法打开文档", "문서를 열 수 없습니다"),
        e("Chưa mở được tài liệu", "Document could not be opened", "ドキュメントを開けませんでした", "无法打开文档", "문서를 열지 못했습니다"),
        e("Chưa tải được tài liệu", "Document could not be downloaded", "ドキュメントをダウンロードできませんでした", "无法下载文档", "문서를 다운로드하지 못했습니다"),
        e("Đang tải tài liệu...", "Downloading document…", "ドキュメントをダウンロード中…", "正在下载文档…", "문서 다운로드 중…"),
        e("Chi tiết sách", "Book details", "本の詳細", "图书详情", "도서 정보"),
        e("Tác giả", "Author", "著者", "作者", "저자"),
        e("Tiến trình", "Progress", "進捗", "进度", "진행률"),
        e("Trạng thái", "Status", "ステータス", "状态", "상태"),
        e("Tùy chọn", "Options", "オプション", "选项", "옵션"),
        e("Ghim", "Pin", "ピン留め", "置顶", "고정"),
        e("Bỏ ghim", "Unpin", "ピン留め解除", "取消置顶", "고정 해제"),
        e("Lưu trữ", "Archive", "アーカイブ", "归档", "보관"),
        e("Bỏ lưu trữ", "Unarchive", "アーカイブ解除", "取消归档", "보관 해제"),
        e("Xóa khỏi thư viện", "Remove from library", "ライブラリから削除", "从书库移除", "라이브러리에서 삭제"),
        e("Sửa thông tin", "Edit details", "詳細を編集", "编辑信息", "정보 편집"),
        e("Đổi trạng thái đọc", "Change reading status", "読書状態を変更", "更改阅读状态", "읽기 상태 변경"),
        e("Gắn thẻ", "Add tags", "タグを付ける", "添加标签", "태그 추가"),
        e("Bộ sưu tập", "Collections", "コレクション", "合集", "컬렉션"),
        e("Thẻ", "Tags", "タグ", "标签", "태그"),
        e("Tạo bộ sưu tập", "Create collection", "コレクションを作成", "创建合集", "컬렉션 만들기"),
        e("Tạo thẻ", "Create tag", "タグを作成", "创建标签", "태그 만들기"),
        e("Tên bộ sưu tập", "Collection name", "コレクション名", "合集名称", "컬렉션 이름"),
        e("Tạo thẻ mới...", "Create a new tag…", "新しいタグを作成…", "创建新标签…", "새 태그 만들기…"),
        e("Dấu trang", "Bookmarks", "ブックマーク", "书签", "북마크"),
        e("Tô sáng", "Highlights", "ハイライト", "高亮", "하이라이트"),
        e("Ghi chú", "Notes", "メモ", "笔记", "노트"),
        e("Đánh dấu trang", "Bookmark", "ブックマーク", "添加书签", "북마크"),
        e("Thêm dấu trang", "Add bookmark", "ブックマークを追加", "添加书签", "북마크 추가"),
        e("Xóa dấu trang", "Remove bookmark", "ブックマークを削除", "删除书签", "북마크 삭제"),
        e("Thêm ghi chú & tô sáng", "Add note and highlight", "メモとハイライトを追加", "添加笔记和高亮", "노트 및 하이라이트 추가"),
        e("Tô sáng hoặc thêm ghi chú", "Highlight or add a note", "ハイライトまたはメモを追加", "高亮或添加笔记", "하이라이트 또는 노트 추가"),
        e("Nội dung ghi chú...", "Note…", "メモ…", "笔记…", "노트…"),
        e("Thêm ghi chú (tùy chọn)...", "Add a note (optional)…", "メモを追加（任意）…", "添加笔记（可选）…", "노트 추가(선택)…"),
        e("Màu tô sáng:", "Highlight color:", "ハイライト色：", "高亮颜色：", "하이라이트 색상:"),
        e("Mục lục", "Table of contents", "目次", "目录", "목차"),
        e("Tìm kiếm trong sách", "Search in book", "本を検索", "在书中搜索", "책에서 검색"),
        e("Tìm trong tài liệu...", "Search document…", "ドキュメントを検索…", "搜索文档…", "문서 검색…"),
        e("Cài đặt đọc tài liệu", "Reading settings", "読書設定", "阅读设置", "읽기 설정"),
        e("Tùy chỉnh đọc", "Reading preferences", "読書設定", "阅读偏好", "읽기 환경설정"),
        e("Giao diện đọc sách", "Reading theme", "読書テーマ", "阅读主题", "읽기 테마"),
        e("Chế độ màu", "Color theme", "カラーテーマ", "颜色主题", "색상 테마"),
        e("Sáng", "Light", "ライト", "浅色", "라이트"),
        e("Vàng giấy", "Sepia", "セピア", "护眼色", "세피아"),
        e("Tối", "Dark", "ダーク", "深色", "다크"),
        e("Cỡ chữ mặc định", "Default text size", "既定の文字サイズ", "默认字号", "기본 글자 크기"),
        e("Kiểu chữ", "Typeface", "書体", "字体", "글꼴"),
        e("Mặc định", "Default", "既定", "默认", "기본값"),
        e("Có chân", "Serif", "セリフ", "衬线", "세리프"),
        e("Không chân", "Sans serif", "サンセリフ", "无衬线", "산세리프"),
        e("Căn lề văn bản", "Text alignment", "文字揃え", "文本对齐", "텍스트 정렬"),
        e("Trái", "Left", "左", "左对齐", "왼쪽"),
        e("Đều", "Justified", "両端揃え", "两端对齐", "양쪽 맞춤"),
        e("Cuộn dọc", "Vertical scrolling", "縦スクロール", "垂直滚动", "세로 스크롤"),
        e("Chế độ cuộn liên tục", "Continuous scrolling", "連続スクロール", "连续滚动", "연속 스크롤"),
        e("Điều khiển & Màn hình", "Controls and display", "操作と画面", "控制与显示", "제어 및 화면"),
        e("Chế độ toàn màn hình", "Full screen", "全画面", "全屏", "전체 화면"),
        e("Giữ màn hình luôn sáng", "Keep screen awake", "画面を常にオン", "保持屏幕常亮", "화면 켜짐 유지"),
        e("Phím âm lượng lật trang", "Turn pages with volume keys", "音量キーでページをめくる", "使用音量键翻页", "볼륨 키로 페이지 넘기기"),
        e("Thông tin đọc sách ở chân trang", "Reading info in footer", "フッターに読書情報を表示", "页脚阅读信息", "바닥글에 읽기 정보 표시"),
        e("Theo hệ thống", "System default", "システム設定", "跟随系统", "시스템 설정"),
        e("Khôi phục cài đặt đọc mặc định", "Restore default reading settings", "読書設定を初期値に戻す", "恢复默认阅读设置", "기본 읽기 설정 복원"),
        e("Quản lý cài đặt", "Manage settings", "設定を管理", "管理设置", "설정 관리"),
        e("Thông tin ứng dụng", "App information", "アプリ情報", "应用信息", "앱 정보"),
        e("Tên ứng dụng", "App name", "アプリ名", "应用名称", "앱 이름"),
        e("Phiên bản", "Version", "バージョン", "版本", "버전"),
        e("Bộ đọc sách", "Reader engine", "リーダーエンジン", "阅读引擎", "리더 엔진"),
        e("Tùy chỉnh đọc sách", "Reading preferences", "読書設定", "阅读偏好", "읽기 환경설정"),
        e("Đăng nhập", "Sign in", "ログイン", "登录", "로그인"),
        e("Đăng xuất", "Sign out", "ログアウト", "退出登录", "로그아웃"),
        e("Tạo tài khoản", "Create account", "アカウントを作成", "创建账户", "계정 만들기"),
        e("Tạo tài khoản mới", "Create a new account", "新しいアカウントを作成", "创建新账户", "새 계정 만들기"),
        e("Đăng ký tài khoản", "Create account", "アカウントを作成", "注册账户", "계정 만들기"),
        e("Email", "Email", "メール", "电子邮件", "이메일"),
        e("Mật khẩu", "Password", "パスワード", "密码", "비밀번호"),
        e("Mật khẩu (tối thiểu 12 ký tự)", "Password (at least 12 characters)", "パスワード（12文字以上）", "密码（至少 12 个字符）", "비밀번호(12자 이상)"),
        e("Xác nhận mật khẩu", "Confirm password", "パスワードを確認", "确认密码", "비밀번호 확인"),
        e("Quên mật khẩu?", "Forgot password?", "パスワードを忘れた場合", "忘记密码？", "비밀번호를 잊으셨나요?"),
        e("Quên mật khẩu", "Reset password", "パスワードを再設定", "重置密码", "비밀번호 재설정"),
        e("Đăng nhập với Google", "Sign in with Google", "Googleでログイン", "使用 Google 登录", "Google로 로그인"),
        e("Tiếp tục với tư cách khách", "Continue as guest", "ゲストとして続ける", "以访客身份继续", "게스트로 계속"),
        e("Bạn đang dùng chế độ khách", "You are using Guest mode", "ゲストモードを使用中です", "你正在使用访客模式", "게스트 모드를 사용 중입니다"),
        e("Đổi tên hiển thị", "Change display name", "表示名を変更", "更改显示名称", "표시 이름 변경"),
        e("Tên hiển thị mới", "New display name", "新しい表示名", "新显示名称", "새 표시 이름"),
        e("Xóa tài khoản", "Delete account", "アカウントを削除", "删除账户", "계정 삭제"),
        e("Xác nhận xóa", "Confirm deletion", "削除を確認", "确认删除", "삭제 확인"),
        e("Đã xác thực", "Verified", "確認済み", "已验证", "인증됨"),
        e("Gửi lại link", "Resend link", "リンクを再送", "重新发送链接", "링크 다시 보내기"),
        e("Đồng bộ nhiều thiết bị", "Multi-device sync", "複数端末の同期", "多设备同步", "여러 기기 동기화"),
        e("Đồng bộ ngay", "Sync now", "今すぐ同期", "立即同步", "지금 동기화"),
        e("Đã đồng bộ", "Synced", "同期済み", "已同步", "동기화됨"),
        e("Đang đồng bộ", "Syncing", "同期中", "正在同步", "동기화 중"),
        e("Không có mạng", "Offline", "オフライン", "离线", "오프라인"),
        e("Bản sao lưu thư viện", "Library backup", "ライブラリのバックアップ", "书库备份", "라이브러리 백업"),
        e("Sao lưu", "Back up", "バックアップ", "备份", "백업"),
        e("Khôi phục", "Restore", "復元", "恢复", "복원"),
        e("Xóa bản sao lưu", "Delete backup", "バックアップを削除", "删除备份", "백업 삭제"),
        e("Nâng cấp Pro", "Upgrade to Pro", "Proにアップグレード", "升级到 Pro", "Pro로 업그레이드"),
        e("Khôi phục giao dịch", "Restore purchases", "購入を復元", "恢复购买", "구매 복원"),
        e("Thanh toán đang chờ xác nhận", "Purchase pending", "購入処理中", "购买待确认", "구매 대기 중"),
        e("Cập nhật trạng thái gói", "Refresh plan status", "プラン状態を更新", "刷新套餐状态", "요금제 상태 새로고침"),
        e("Bộ nhớ đọc", "Reading Memory", "リーディングメモリー", "阅读记忆", "리딩 메모리"),
        e("Trở lại những điều bạn muốn nhớ", "Return to what you want to remember", "覚えておきたいことに戻る", "回顾你想记住的内容", "기억하고 싶은 내용으로 돌아가기"),
        e("Ôn tập", "Review", "復習", "复习", "복습"),
        e("Ôn tập hàng ngày", "Daily review", "毎日の復習", "每日复习", "매일 복습"),
        e("Ôn tập ngắt quãng", "Spaced review", "間隔反復", "间隔复习", "간격 반복"),
        e("Tìm kiếm tri thức", "Knowledge Search", "ナレッジ検索", "知识搜索", "지식 검색"),
        e("Thống kê đọc", "Reading statistics", "読書統計", "阅读统计", "읽기 통계"),
        e("Toàn bộ Ghi chú & Đoạn trích", "All notes and highlights", "すべてのメモとハイライト", "全部笔记与高亮", "모든 노트 및 하이라이트"),
        e("Ghi chú & Đoạn trích gần đây", "Recent notes and highlights", "最近のメモとハイライト", "最近的笔记与高亮", "최근 노트 및 하이라이트"),
        e("Không tìm thấy kết quả nào", "No results found", "結果が見つかりません", "未找到结果", "검색 결과가 없습니다"),
        e("Không có nội dung nào phù hợp", "No matching content", "一致するコンテンツはありません", "没有匹配的内容", "일치하는 콘텐츠가 없습니다"),
        e("Hôm nay", "Today", "今日", "今天", "오늘"),
        e("7 ngày qua", "Past 7 days", "過去7日間", "过去 7 天", "지난 7일"),
        e("30 ngày qua", "Past 30 days", "過去30日間", "过去 30 天", "지난 30일"),
        e("Tổng phiên đọc", "Total reading sessions", "読書セッション合計", "阅读次数总计", "전체 읽기 세션"),
        e("Chưa nhớ", "Again", "もう一度", "未记住", "다시"),
        e("Khó", "Hard", "難しい", "困难", "어려움"),
        e("Tốt", "Good", "良い", "良好", "좋음"),
        e("Dễ", "Easy", "簡単", "简单", "쉬움"),
        e("Chia sẻ", "Share", "共有", "分享", "공유"),
        e("Sao chép", "Copy", "コピー", "复制", "복사"),
        e("Xuất Markdown", "Export Markdown", "Markdownを書き出す", "导出 Markdown", "Markdown 내보내기"),
        e("Đã hiểu", "Got it", "了解", "知道了", "확인"),
        e("Không thể mở sách", "Unable to open book", "本を開けません", "无法打开图书", "도서를 열 수 없습니다"),
        e("Không tìm thấy sách", "Book not found", "本が見つかりません", "未找到图书", "도서를 찾을 수 없습니다"),
        e("Không tìm thấy tài liệu", "Document not found", "ドキュメントが見つかりません", "未找到文档", "문서를 찾을 수 없습니다"),
        e("Đã thêm phông chữ: ", "Font added: ", "フォントを追加しました：", "已添加字体：", "글꼴 추가됨: "),
        e("Không thể thêm phông chữ", "Unable to add font", "フォントを追加できません", "无法添加字体", "글꼴을 추가할 수 없습니다"),
        e("Đã sao chép trích dẫn", "Quote copied", "引用をコピーしました", "引用已复制", "인용문이 복사되었습니다"),
        e("Đã xuất dữ liệu ghi chú thành công!", "Notes exported successfully!", "メモを書き出しました。", "笔记导出成功！", "노트를 내보냈습니다!"),
        e("Lỗi khi xuất tệp Markdown", "Unable to export Markdown", "Markdownを書き出せません", "无法导出 Markdown", "Markdown을 내보낼 수 없습니다"),
        e("Không thể mở tệp để ghi", "Unable to open the destination file", "保存先のファイルを開けません", "无法打开目标文件", "대상 파일을 열 수 없습니다"),
        e("Bạn đang dùng chế độ khách", "You are using Guest mode", "ゲストモードを使用中です", "你正在使用访客模式", "게스트 모드를 사용 중입니다"),
        e("Đăng nhập để sao lưu và khôi phục thư viện của bạn", "Sign in to back up and restore your library", "ライブラリのバックアップと復元にはログインしてください", "登录以备份和恢复书库", "라이브러리를 백업하고 복원하려면 로그인하세요"),
        e("Yêu cầu xác thực Email", "Email verification required", "メール認証が必要です", "需要验证电子邮件", "이메일 인증 필요"),
        e("Email đã xác thực", "Email verified", "メール確認済み", "电子邮件已验证", "이메일 인증됨"),
        e("Máy chủ ngoại tuyến. Tính năng ngoại tuyến vẫn hoạt động bình thường.", "Server unavailable. Offline features still work normally.", "サーバーに接続できません。オフライン機能は引き続き利用できます。", "服务器离线。离线功能仍可正常使用。", "서버가 오프라인입니다. 오프라인 기능은 계속 사용할 수 있습니다."),
        e("Đăng xuất chuyển sang thư viện Khách riêng biệt. Dữ liệu tài khoản vẫn được giữ để dùng tiếp khi đăng nhập lại.", "Signing out switches to a separate Guest library. Your account data remains available when you sign in again.", "ログアウトすると別のゲストライブラリに切り替わります。アカウントのデータは次回ログイン時まで保持されます。", "退出登录后将切换到独立的访客书库。再次登录时账户数据仍会保留。", "로그아웃하면 별도의 게스트 라이브러리로 전환됩니다. 계정 데이터는 다시 로그인할 때까지 유지됩니다."),
        e("Sao lưu và khôi phục sách, ghi chú, tiến độ, thẻ và bộ sưu tập.", "Back up and restore books, notes, progress, tags, and collections.", "本、メモ、進捗、タグ、コレクションをバックアップ・復元します。", "备份和恢复图书、笔记、进度、标签及合集。", "도서, 노트, 진행률, 태그 및 컬렉션을 백업하고 복원합니다."),
        e("Sao lưu thư viện?", "Back up library?", "ライブラリをバックアップしますか？", "备份书库？", "라이브러리를 백업할까요?"),
        e("Khôi phục thư viện?", "Restore library?", "ライブラリを復元しますか？", "恢复书库？", "라이브러리를 복원할까요?"),
        e("Xóa bản sao lưu?", "Delete backup?", "バックアップを削除しますか？", "删除备份？", "백업을 삭제할까요?"),
        e("Bản sao lưu trên đám mây sẽ bị xóa. Dữ liệu trong máy vẫn được giữ.", "The cloud backup will be deleted. Data on this device will remain.", "クラウドのバックアップを削除します。端末内のデータは保持されます。", "云端备份将被删除，设备上的数据仍会保留。", "클라우드 백업이 삭제됩니다. 기기의 데이터는 유지됩니다."),
        e("Thư viện hiện tại sẽ được thay bằng bản sao lưu trên tài khoản này. Hãy sao lưu dữ liệu cần giữ trước khi tiếp tục.", "The current library will be replaced by this account's backup. Back up anything you want to keep before continuing.", "現在のライブラリはこのアカウントのバックアップに置き換えられます。続行前に必要なデータをバックアップしてください。", "当前书库将替换为此账户的备份。继续前请备份需要保留的数据。", "현재 라이브러리가 이 계정의 백업으로 교체됩니다. 계속하기 전에 보관할 데이터를 백업하세요."),
        e("Sách, ghi chú, tiến độ và bộ sưu tập trên máy sẽ được tải lên tài khoản đang đăng nhập. Bản sao lưu gần nhất sẽ được thay thế.", "Books, notes, progress, and collections on this device will be uploaded to the signed-in account, replacing its latest backup.", "端末内の本、メモ、進捗、コレクションをログイン中のアカウントへアップロードし、最新のバックアップを置き換えます。", "设备上的图书、笔记、进度与合集将上传到当前账户，并替换最近的备份。", "기기의 도서, 노트, 진행률 및 컬렉션을 로그인한 계정에 업로드하며 최근 백업을 교체합니다."),
        e("Free luôn giữ quyền đọc, nhập tài liệu, dấu trang, ghi chú và thư viện offline. Pro thêm đồng bộ và lưu trữ đám mây.", "Free always includes reading, document import, bookmarks, notes, and an offline library. Pro adds sync and cloud storage.", "Freeでは読書、インポート、ブックマーク、メモ、オフラインライブラリを常に利用できます。Proでは同期とクラウド保存が追加されます。", "免费版始终支持阅读、导入、书签、笔记和离线书库。Pro 增加同步与云存储。", "Free에서는 읽기, 문서 가져오기, 북마크, 노트 및 오프라인 라이브러리를 계속 사용할 수 있습니다. Pro는 동기화와 클라우드 저장소를 추가합니다."),
        e("Pro chưa mở bán. Bạn vẫn có thể đọc và quản lý tài liệu trên máy với Free.", "Pro is not available for purchase yet. You can continue reading and managing local documents with Free.", "Proはまだ購入できません。Freeで端末内のドキュメントを引き続き読んで管理できます。", "Pro 尚未开放购买。你仍可使用免费版阅读和管理本地文档。", "Pro는 아직 구매할 수 없습니다. Free로 로컬 문서를 계속 읽고 관리할 수 있습니다."),
        e("Pro không hoạt động. Tài liệu và thay đổi chưa đồng bộ vẫn được giữ trên máy.", "Pro is inactive. Local documents and unsynced changes remain on this device.", "Proは無効です。ローカルのドキュメントと未同期の変更は端末に保持されます。", "Pro 未激活。本地文档和未同步更改仍保留在设备上。", "Pro가 비활성 상태입니다. 로컬 문서와 동기화되지 않은 변경 사항은 기기에 유지됩니다."),
        e("Khôi phục cài đặt?", "Restore settings?", "設定を初期値に戻しますか？", "恢复设置？", "설정을 복원할까요?"),
        e("Bạn có chắc chắn muốn đặt lại toàn bộ tùy chỉnh giao diện và phông chữ đọc sách về mặc định?", "Reset all reading appearance and font preferences to their defaults?", "読書画面とフォントの設定をすべて初期値に戻しますか？", "将所有阅读外观和字体设置恢复为默认值？", "모든 읽기 화면 및 글꼴 설정을 기본값으로 되돌릴까요?"),
        e("Đặt lại", "Reset", "リセット", "重置", "재설정"),
        e("Đặt lại cỡ chữ 100%, giao diện sáng và kiểu chữ mặc định", "Reset to 100% text size, light theme, and default typeface", "文字サイズ100%、ライトテーマ、既定の書体に戻します", "重置为 100% 字号、浅色主题和默认字体", "글자 크기 100%, 라이트 테마 및 기본 글꼴로 재설정"),
        e("Xác nhận xóa tài khoản?", "Delete account?", "アカウントを削除しますか？", "删除账户？", "계정을 삭제할까요?"),
        e("Hành động này sẽ xóa vĩnh viễn tài khoản của bạn và toàn bộ dữ liệu hồ sơ liên kết trên máy chủ. Sách đã tải về trên máy và tiến trình đọc cục bộ sẽ được giữ lại.", "This permanently deletes your account and linked profile data from the server. Downloaded books and local reading progress remain on this device.", "アカウントと関連プロフィールデータをサーバーから完全に削除します。ダウンロード済みの本と端末内の読書進捗は保持されます。", "此操作会永久删除服务器上的账户及关联资料。已下载图书和本地阅读进度仍保留在设备上。", "서버에서 계정과 연결된 프로필 데이터를 영구 삭제합니다. 다운로드한 도서와 로컬 읽기 진행률은 기기에 유지됩니다."),
        e("Thư viện tài liệu của bạn đang trống", "Your document library is empty", "ドキュメントライブラリは空です", "你的文档书库为空", "문서 라이브러리가 비어 있습니다"),
        e("Nhập tài liệu EPUB hoặc PDF từ thiết bị hoặc tải từ liên kết để bắt đầu.", "Import an EPUB or PDF from your device, or download one from a link to get started.", "端末からEPUBまたはPDFをインポートするか、リンクからダウンロードして始めましょう。", "从设备导入 EPUB 或 PDF，或通过链接下载以开始使用。", "기기에서 EPUB 또는 PDF를 가져오거나 링크에서 다운로드해 시작하세요."),
        e("Bộ lọc hiện tại chưa có kết quả. Chọn Tất cả ở phía trên để xem lại thư viện.", "No results match the current filter. Select All above to view your library.", "現在のフィルターに一致する結果はありません。上の「すべて」を選択してください。", "当前筛选条件没有结果。请选择上方的“全部”查看书库。", "현재 필터와 일치하는 결과가 없습니다. 위에서 전체를 선택하세요."),
        e("Không có tài liệu chưa phân loại", "No unsorted documents", "未分類のドキュメントはありません", "没有未分类文档", "미분류 문서가 없습니다"),
        e("Mở Tất cả để xem thư viện, hoặc thêm tài liệu mới bằng nút phía dưới.", "Open All to view your library, or add a document with the button below.", "「すべて」でライブラリを表示するか、下のボタンからドキュメントを追加してください。", "打开“全部”查看书库，或使用下方按钮添加文档。", "전체를 열어 라이브러리를 보거나 아래 버튼으로 문서를 추가하세요."),
        e("Chưa có bộ sưu tập nào", "No collections yet", "コレクションはまだありません", "暂无合集", "컬렉션이 아직 없습니다"),
        e("Chưa có bộ sưu tập nào.", "No collections yet.", "コレクションはまだありません。", "暂无合集。", "컬렉션이 아직 없습니다."),
        e("Chưa có thẻ nào", "No tags yet", "タグはまだありません", "暂无标签", "태그가 아직 없습니다"),
        e("Chưa có sách để giới thiệu", "No books to recommend yet", "おすすめできる本はまだありません", "暂无推荐图书", "추천할 도서가 아직 없습니다"),
        e("Sách được đánh dấu yêu thích sẽ xuất hiện ở đây", "Books you favorite will appear here", "お気に入りにした本がここに表示されます", "收藏的图书会显示在这里", "즐겨찾기한 도서가 여기에 표시됩니다"),
        e("Sách yêu thích", "Favorite books", "お気に入りの本", "收藏图书", "즐겨찾는 도서"),
        e("Chỉ chấp nhận liên kết HTTPS an toàn", "Only secure HTTPS links are accepted", "安全なHTTPSリンクのみ使用できます", "仅接受安全的 HTTPS 链接", "안전한 HTTPS 링크만 허용됩니다"),
        e("Nhập liên kết HTTPS tới tệp tài liệu hoặc trang bài viết. Với trang web, app chỉ giữ nội dung chính và loại bỏ liên kết, menu, quảng cáo.", "Enter an HTTPS link to a document or article. For web pages, the app keeps the main content and removes links, menus, and ads.", "ドキュメントまたは記事のHTTPSリンクを入力してください。Webページでは本文のみを残し、リンク、メニュー、広告を除去します。", "输入文档或文章页面的 HTTPS 链接。网页仅保留正文，并移除链接、菜单与广告。", "문서 또는 글의 HTTPS 링크를 입력하세요. 웹페이지에서는 본문만 유지하고 링크, 메뉴 및 광고를 제거합니다."),
        e("Chọn tệp EPUB hoặc PDF có sẵn trên máy", "Choose an EPUB or PDF on this device", "端末内のEPUBまたはPDFを選択", "选择设备上的 EPUB 或 PDF", "기기의 EPUB 또는 PDF 선택"),
        e("Tải tài liệu trực tiếp từ liên kết mạng an toàn", "Download a document from a secure web link", "安全なWebリンクからドキュメントをダウンロード", "从安全网页链接下载文档", "안전한 웹 링크에서 문서 다운로드"),
        e("Cuốn sách này không có mục lục", "This book has no table of contents", "この本には目次がありません", "此书没有目录", "이 도서에는 목차가 없습니다"),
        e("Không có văn bản để chọn", "No text available to select", "選択できるテキストがありません", "没有可选择的文本", "선택할 텍스트가 없습니다"),
        e("Chưa có dấu trang nào.\nNhấn biểu tượng dấu trang trên thanh tiêu đề để lưu vị trí đọc.", "No bookmarks yet.\nTap the bookmark icon in the top bar to save your reading position.", "ブックマークはまだありません。\n上部のブックマークアイコンをタップして読書位置を保存できます。", "暂无书签。\n点击顶部栏的书签图标保存阅读位置。", "북마크가 아직 없습니다.\n상단 표시줄의 북마크 아이콘을 눌러 읽기 위치를 저장하세요."),
        e("Chưa có đoạn văn nào được tô sáng.\nChọn đoạn văn trong sách để tô sáng hoặc ghi chú.", "No highlights yet.\nSelect text in the book to highlight it or add a note.", "ハイライトはまだありません。\n本のテキストを選択してハイライトまたはメモを追加できます。", "暂无高亮。\n选择书中文字即可高亮或添加笔记。", "하이라이트가 아직 없습니다.\n도서의 텍스트를 선택해 하이라이트하거나 노트를 추가하세요."),
        e("Chưa có ghi chú nào.\nChọn đoạn văn và chọn Ghi chú để lưu ý tưởng của bạn.", "No notes yet.\nSelect text and choose Note to save your thoughts.", "メモはまだありません。\nテキストを選択して「メモ」を選ぶと考えを保存できます。", "暂无笔记。\n选择文字并点击“笔记”保存想法。", "노트가 아직 없습니다.\n텍스트를 선택하고 노트를 눌러 생각을 저장하세요."),
        e("Tính năng tìm kiếm toàn văn hiện tại chỉ hỗ trợ định dạng EPUB.\nĐịnh dạng tài liệu hiện tại không hỗ trợ bộ chỉ mục tìm kiếm.", "Full-text search currently supports EPUB only.\nThis document format does not support a search index.", "全文検索は現在EPUBのみ対応しています。\nこのドキュメント形式は検索インデックスに対応していません。", "全文搜索目前仅支持 EPUB。\n当前文档格式不支持搜索索引。", "전체 텍스트 검색은 현재 EPUB만 지원합니다.\n이 문서 형식은 검색 색인을 지원하지 않습니다."),
        e("Tìm kiếm trong toàn bộ tri thức của bạn", "Search across your knowledge", "すべてのナレッジを検索", "搜索你的全部知识", "전체 지식 검색"),
        e("Tìm tri thức, tài liệu, ghi chú...", "Search knowledge, documents, notes…", "ナレッジ、ドキュメント、メモを検索…", "搜索知识、文档和笔记…", "지식, 문서 및 노트 검색…"),
        e("Tìm tài liệu, đoạn trích, ghi chú, đánh dấu trang hoặc nội dung", "Search documents, highlights, notes, bookmarks, or content", "ドキュメント、ハイライト、メモ、ブックマーク、本文を検索", "搜索文档、高亮、笔记、书签或正文", "문서, 하이라이트, 노트, 북마크 또는 콘텐츠 검색"),
        e("Tìm đoạn tô sáng, ghi chú hoặc ôn lại những ý đã lưu khi đọc.", "Find highlights and notes, or review ideas you saved while reading.", "ハイライトやメモを探し、読書中に保存した内容を復習できます。", "查找高亮和笔记，或复习阅读时保存的想法。", "하이라이트와 노트를 찾거나 읽으며 저장한 내용을 복습하세요."),
        e("Chưa có ghi chú hoặc đoạn trích nào.\nHãy chọn văn bản khi đọc để lưu giữ tri thức!", "No notes or highlights yet.\nSelect text while reading to save useful ideas.", "メモやハイライトはまだありません。\n読書中にテキストを選択して知識を保存しましょう。", "暂无笔记或高亮。\n阅读时选择文字以保存有用内容。", "노트나 하이라이트가 아직 없습니다.\n읽는 동안 텍스트를 선택해 유용한 내용을 저장하세요."),
        e("Bạn đã hoàn thành tất cả mục ôn tập hôm nay!", "You completed every review for today!", "今日の復習をすべて完了しました。", "你已完成今天的全部复习！", "오늘의 모든 복습을 완료했습니다!"),
        e("Đánh giá mức độ nhớ của bạn:", "How well did you remember?", "どのくらい覚えていましたか？", "你的记忆程度如何？", "얼마나 잘 기억했나요?"),
        e("Xem trong tài liệu gốc", "View in source document", "元のドキュメントで表示", "在原文档中查看", "원본 문서에서 보기"),
        e("Thống kê & Thói quen đọc", "Reading statistics and habits", "読書統計と習慣", "阅读统计与习惯", "읽기 통계 및 습관"),
        e("Tổng quan thời gian đọc", "Reading time overview", "読書時間の概要", "阅读时长概览", "읽기 시간 개요"),
        e("Tài liệu đọc nhiều nhất", "Most-read documents", "よく読んだドキュメント", "最常阅读的文档", "가장 많이 읽은 문서"),
        e("Không có kết quả phù hợp", "No matching results", "一致する結果はありません", "没有匹配的结果", "일치하는 결과가 없습니다"),
        e("Tìm kiếm tri thức trong toàn bộ thư viện...", "Search knowledge across your library…", "ライブラリ全体のナレッジを検索…", "搜索整个书库中的知识…", "라이브러리 전체의 지식 검색…"),
        e("Đã đọc xong", "Finished reading", "読了", "已读完", "읽기 완료"),
        e("Ghim lên đầu", "Pin to top", "先頭に固定", "置顶", "맨 위에 고정"),
        e("Đã phân loại", "Mark as organized", "整理済みにする", "标记为已分类", "정리 완료로 표시"),
        e("Hộp thư đến", "Move to inbox", "受信箱に移動", "移至收件箱", "받은 문서함으로 이동"),
        e("Xóa tài liệu", "Delete document", "ドキュメントを削除", "删除文档", "문서 삭제"),
        e("Mới đọc", "Recently read", "最近読んだ", "最近阅读", "최근 읽음"),
        e("Đã ghim", "Pinned", "固定済み", "已置顶", "고정됨"),
        e("Tiến độ đọc", "Reading progress", "読書の進捗", "阅读进度", "읽기 진행률"),
        e("Mới thêm", "Recently added", "最近追加した", "最近添加", "최근 추가"),
        e("Tên (A-Z)", "Title (A–Z)", "タイトル（A–Z）", "书名（A–Z）", "제목 (A–Z)"),
        e("Tên (Z-A)", "Title (Z–A)", "タイトル（Z–A）", "书名（Z–A）", "제목 (Z–A)"),
        e("Hình ảnh", "Images", "画像", "图片", "이미지"),
        e("Từ Web", "From the web", "ウェブから", "来自网页", "웹에서 가져옴"),
        e("Chưa tải được tài liệu. Kiểm tra kết nối và dung lượng trống rồi thử lại. Sách đã lưu vẫn được giữ.", "Could not download. Check your connection and free storage, then try again. Saved books are safe.", "ダウンロードできませんでした。接続と空き容量を確認して再試行してください。保存済みの本は保持されています。", "无法下载。请检查网络和剩余空间后重试。已保存的图书不会丢失。", "다운로드하지 못했습니다. 연결과 여유 공간을 확인한 후 다시 시도하세요. 저장된 도서는 유지됩니다."),
        e("Ghi nhớ", "Memory", "メモリー", "记忆", "메모리"),
        e("Đồng bộ & Sao lưu", "Sync & Backup", "同期とバックアップ", "同步与备份", "동기화 및 백업"),
        e("Gói dịch vụ", "Subscription plan", "プラン", "服务套餐", "요금제"),
        e("Trải nghiệm đọc", "Reading experience", "読書体験", "阅读体验", "읽기 환경"),
        e("Thông tin ứng dụng", "App information", "アプリ情報", "应用信息", "앱 정보"),
        e("Đã xác nhận gói thành công", "Plan verified successfully", "プランの確認が完了しました", "套餐已成功确认", "요금제가 성공적으로 확인되었습니다"),
        e("Đã kiểm tra gói; một số giao dịch chưa khôi phục được hoặc thuộc tài khoản khác.", "Plan checked; some purchases could not be restored or belong to another account.", "プランを確認しました。一部の取引を復元できないか、別のアカウントに属しています。", "已检查套餐；部分交易无法恢复或属于其他账户。", "요금제를 확인했습니다. 일부 거래를 복원할 수 없거나 다른 계정에 속해 있습니다."),
        e("Chọn tệp EPUB, PDF, TXT, MD, HTML, DOCX, CBZ hoặc ảnh", "Choose EPUB, PDF, TXT, MD, HTML, DOCX, CBZ, or image files", "EPUB、PDF、TXT、MD、HTML、DOCX、CBZ、または画像ファイルを選択", "选择 EPUB、PDF、TXT、MD、HTML、DOCX、CBZ 或图片文件", "EPUB, PDF, TXT, MD, HTML, DOCX, CBZ 또는 이미지 파일 선택"),
        e("Thêm tài liệu từ thiết bị hoặc tải từ liên kết để bắt đầu (hỗ trợ EPUB, PDF, TXT, MD, HTML, DOCX, CBZ, ảnh).", "Add documents from your device or download from a link (supports EPUB, PDF, TXT, MD, HTML, DOCX, CBZ, images).", "端末からドキュメントを追加するかリンクからダウンロードして始めましょう（EPUB、PDF、TXT、MD、HTML、DOCX、CBZ、画像に対応）。", "从设备添加文档或通过链接下载以开始（支持 EPUB、PDF、TXT、MD、HTML、DOCX、CBZ、图片）。", "기기에서 문서를 추가하거나 링크에서 다운로드해 시작하세요(EPUB, PDF, TXT, MD, HTML, DOCX, CBZ, 이미지 지원)."),
        e("Không gian đọc", "Reading Workspace", "読書ワークスペース", "阅读工作区", "읽기 공간"),
        e("Tài liệu gần đây", "Recent documents", "最近のドキュメント", "最近文档", "최근 문서"),
        e("Ghi nhớ gần đây", "Recent notes & highlights", "最近のメモとハイライト", "最近笔记与高亮", "최근 노트 및 하이라이트"),
        e("Khám phá thêm", "Explore more", "さらに見つける", "发现更多", "더 둘러보기"),
        e("Tiếp tục hành trình đọc và tích lũy tri thức", "Continue your reading and knowledge journey", "読書と知識の記録を続けましょう", "继续你的阅读与知识之旅", "읽기와 지식 여정을 계속하세요"),
        e("Dữ liệu trên máy được lưu riêng theo tài khoản. Đăng xuất sẽ chuyển về thư viện Khách.", "Local data is isolated per account. Signing out switches to the Guest library.", "端末のデータはアカウントごとに分離されています。ログアウトするとゲスト用ライブラリに切り替わります。", "设备数据按账户隔离。退出登录将切换至访客书库。", "기기 데이터는 계정별로 분리됩니다. 로그아웃하면 게스트 라이브러리로 전환됩니다."),
        e("Đăng nhập để đồng bộ và sao lưu thư viện giữa các thiết bị", "Sign in to sync and back up your library across devices", "複数端末で同期・バックアップするにはログインしてください", "登录以跨设备同步和备份书库", "여러 기기 간 라이브러리를 동기화하고 백업하려면 로그인하세요"),
        e("Đăng nhập tài khoản để tự động đồng bộ tiến độ đọc và sao lưu thư viện giữa các thiết bị.", "Sign in to automatically sync reading progress and back up your library across devices.", "ログインすると、読書進捗の自動同期や複数端末でのバックアップが利用できます。", "登录账户以自动同步阅读进度并在设备间备份书库。", "계정에 로그인하면 읽기 진행률이 자동 동기화되고 기기 간 라이브러리가 백업됩니다."),
        e("Sao lưu & Khôi phục", "Backup & Restore", "バックアップと復元", "备份与恢复", "백업 및 복원"),
        e("Lưu trữ toàn bộ sách, ghi chú, tiến độ đọc và bộ sưu tập lên tài khoản.", "Save all books, notes, reading progress, and collections to your account.", "すべての本、メモ、読書進捗、コレクションをアカウントに保存します。", "将所有图书、笔记、阅读进度和合集保存至账户。", "모든 도서, 노트, 읽기 진행률 및 컬렉션을 계정에 저장합니다."),
        e("Xóa bản sao lưu trên đám mây", "Delete cloud backup", "クラウドバックアップを削除", "删除云端备份", "클라우드 백업 삭제"),
        e("Gói hiện tại: Pro", "Current plan: Pro", "現在のプラン：Pro", "当前套餐：Pro", "현재 요금제: Pro"),
        e("Gói hiện tại: Free", "Current plan: Free", "現在のプラン：Free", "当前套餐：Free", "현재 요금제: Free"),
        e("Làm mới trạng thái gói", "Refresh plan status", "プラン状態を更新", "刷新套餐状态", "요금제 상태 새로고침"),
        e("Tiếp tục đọc", "Continue reading", "読書を続ける", "继续阅读", "계속 읽기"),
        e("Sách tuyển chọn", "Featured books", "注目の本", "精选图书", "추천 도서"),
        e("Khám phá theo thể loại", "Explore by category", "カテゴリーで探す", "按分类浏览", "카테고리별 둘러보기"),
        e("Chào mừng bạn đến với Không gian đọc", "Welcome to your Reading Workspace", "読書ワークスペースへようこそ", "欢迎来到阅读工作区", "읽기 공간에 오신 것을 환영합니다"),
        e("Thêm sách hoặc tài liệu từ thiết bị để bắt đầu trải nghiệm đọc cá nhân.", "Add books or documents from your device to start your personal reading experience.", "端末から本やドキュメントを追加して、あなただけの読書を始めましょう。", "从设备添加图书或文档以开始个性化阅读体验。", "기기에서 도서나 문서를 추가해 나만의 읽기를 시작하세요."),
        e("Mở Thư viện tài liệu", "Open Document Library", "ドキュメントライブラリを開く", "打开文档书库", "문서 라이브러리 열기")
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
            return choose(language, "$n documents", "$n 件のドキュメント", "$n 个文档", "문서 ${n}개")
        }
        match("Trang (\\d+)")?.let { m ->
            val n = m.groupValues[1]
            return choose(language, "Page $n", "$n ページ", "第 $n 页", "${n}페이지")
        }
        match("Tìm thấy (\\d+) kết quả")?.let { m ->
            val n = m.groupValues[1]
            return choose(language, "$n results found", "$n 件見つかりました", "找到 $n 个结果", "결과 ${n}개")
        }
        match("(\\d+) kết quả cho \"(.+)\"")?.let { m ->
            val count = m.groupValues[1]
            val query = m.groupValues[2]
            return choose(language, "$count results for “$query”", "「$query」の結果：$count 件", "“$query”的结果：$count 个", "‘$query’ 검색 결과 ${count}개")
        }
        match("(\\d+) phiên")?.let { m ->
            val count = m.groupValues[1]
            return choose(language, "$count sessions", "$count セッション", "$count 次", "세션 ${count}개")
        }
        match("(\\d+) nội dung cần ôn tập hôm nay")?.let { m ->
            val count = m.groupValues[1]
            return choose(language, "$count items to review today", "今日の復習：$count 件", "今天需复习 $count 项", "오늘 복습할 항목 ${count}개")
        }
        match("Bắt đầu ôn tập \\((\\d+)\\)")?.let { m ->
            val count = m.groupValues[1]
            return choose(language, "Start review ($count)", "復習を開始（$count）", "开始复习（$count）", "복습 시작 ($count)")
        }
        match("Thẻ (\\d+) / (\\d+)")?.let { m ->
            return choose(language, "Card ${m.groupValues[1]} / ${m.groupValues[2]}", "カード ${m.groupValues[1]} / ${m.groupValues[2]}", "卡片 ${m.groupValues[1]} / ${m.groupValues[2]}", "카드 ${m.groupValues[1]} / ${m.groupValues[2]}")
        }
        match("Đang tải: (.+)")?.let { m ->
            return choose(language, "Downloading: ${m.groupValues[1]}", "ダウンロード中：${m.groupValues[1]}", "正在下载：${m.groupValues[1]}", "다운로드 중: ${m.groupValues[1]}")
        }
        match("Cập nhật \\(phiên bản (.+)\\)")?.let { m ->
            return choose(language, "Update (version ${m.groupValues[1]})", "更新（バージョン ${m.groupValues[1]}）", "更新（版本 ${m.groupValues[1]}）", "업데이트(버전 ${m.groupValues[1]})")
        }
        match("Chọn bộ sưu tập cho (\\d+) tài liệu:")?.let { m ->
            return choose(language, "Choose collections for ${m.groupValues[1]} documents:", "${m.groupValues[1]} 件のコレクションを選択：", "为 ${m.groupValues[1]} 个文档选择合集：", "문서 ${m.groupValues[1]}개의 컬렉션 선택:")
        }
        match("Chọn thẻ để gắn cho (\\d+) tài liệu đã chọn:")?.let { m ->
            return choose(language, "Choose tags for ${m.groupValues[1]} selected documents:", "選択した ${m.groupValues[1]} 件のタグを選択：", "为选中的 ${m.groupValues[1]} 个文档选择标签：", "선택한 문서 ${m.groupValues[1]}개의 태그 선택:")
        }
        match("Chọn (?:các bộ sưu tập|thẻ) cho: \"(.+)\"")?.let { m ->
            return choose(language, "Choose organization for “${m.groupValues[1]}”", "「${m.groupValues[1]}」を整理", "整理“${m.groupValues[1]}”", "‘${m.groupValues[1]}’ 정리")
        }
        match("Chưa có tài liệu trong (.+)")?.let { m ->
            return choose(language, "No documents in ${m.groupValues[1]}", "${m.groupValues[1]} にドキュメントはありません", "${m.groupValues[1]} 中没有文档", "${m.groupValues[1]}에 문서가 없습니다")
        }
        match("Chưa phân loại \\((\\d+)\\)")?.let { m ->
            return choose(language, "Unsorted (${m.groupValues[1]})", "未分類（${m.groupValues[1]}）", "未分类（${m.groupValues[1]}）", "미분류 (${m.groupValues[1]})")
        }
        match("Đã (bỏ ghim|bỏ lưu trữ|gắn thẻ cho|ghim|lưu trữ) (\\d+) tài liệu")?.let { m ->
            val action = when (m.groupValues[1]) {
                "bỏ ghim" -> "unpinned"
                "bỏ lưu trữ" -> "unarchived"
                "gắn thẻ cho" -> "tagged"
                "ghim" -> "pinned"
                else -> "archived"
            }
            return choose(language, "${m.groupValues[2]} documents $action", "${m.groupValues[2]} 件を更新しました", "已更新 ${m.groupValues[2]} 个文档", "문서 ${m.groupValues[2]}개를 업데이트했습니다")
        }
        match("Đã thêm (\\d+) tài liệu vào bộ sưu tập")?.let { m ->
            return choose(language, "Added ${m.groupValues[1]} documents to collections", "${m.groupValues[1]} 件をコレクションに追加しました", "已将 ${m.groupValues[1]} 个文档添加到合集", "문서 ${m.groupValues[1]}개를 컬렉션에 추가했습니다")
        }
        match("Đã (?:cập nhật trạng thái|đánh dấu): (.+)")?.let { m ->
            val status = translate(m.groupValues[1], language)
            return choose(language, "Status updated: $status", "状態を更新：$status", "状态已更新：$status", "상태 업데이트: $status")
        }
        match("Đã chọn: (\\d+)")?.let { m ->
            return choose(language, "Selected: ${m.groupValues[1]}", "選択：${m.groupValues[1]}", "已选择：${m.groupValues[1]}", "선택: ${m.groupValues[1]}")
        }
        match("Đã đổi tên thành: (.+)")?.let { m ->
            return choose(language, "Renamed to: ${m.groupValues[1]}", "名前を変更：${m.groupValues[1]}", "已重命名为：${m.groupValues[1]}", "이름 변경: ${m.groupValues[1]}")
        }
        match("Đã tạo (bộ sưu tập|thẻ): (.+)")?.let { m ->
            return choose(language, "Created ${m.groupValues[2]}", "${m.groupValues[2]} を作成しました", "已创建 ${m.groupValues[2]}", "${m.groupValues[2]} 생성됨")
        }
        match("Gốc: (.+)")?.let { m ->
            return choose(language, "Original: ${m.groupValues[1]}", "元：${m.groupValues[1]}", "原始：${m.groupValues[1]}", "원본: ${m.groupValues[1]}")
        }
        match("Kiểm tra hộp thư (.+) hoặc nhấn Gửi lại link\\.")?.let { m ->
            return choose(language, "Check ${m.groupValues[1]} or tap Resend link.", "${m.groupValues[1]} を確認するか、リンクを再送してください。", "请检查 ${m.groupValues[1]}，或点击重新发送链接。", "${m.groupValues[1]}을 확인하거나 링크 다시 보내기를 누르세요.")
        }
        match("Lần ôn: (\\d+)")?.let { m ->
            return choose(language, "Reviews: ${m.groupValues[1]}", "復習回数：${m.groupValues[1]}", "复习次数：${m.groupValues[1]}", "복습 횟수: ${m.groupValues[1]}")
        }
        match("(?:Lỗi lưu ảnh bìa|Lỗi xác thực|Máy chủ backend chưa khả dụng|Tải thất bại): (.+)")?.let { m ->
            return choose(language, "Error: ${m.groupValues[1]}", "エラー：${m.groupValues[1]}", "错误：${m.groupValues[1]}", "오류: ${m.groupValues[1]}")
        }
        match("Tài liệu: \"(.+)\" đã có trong thư viện của bạn\\.")?.let { m ->
            return choose(language, "“${m.groupValues[1]}” is already in your library.", "「${m.groupValues[1]}」は既にライブラリにあります。", "“${m.groupValues[1]}”已在你的书库中。", "‘${m.groupValues[1]}’은(는) 이미 라이브러리에 있습니다.")
        }
        match("Tài liệu: \"(.+)\" và toàn bộ dấu trang, ghi chú, tiến độ đọc sẽ bị xóa vĩnh viễn khỏi thiết bị\\.")?.let { m ->
            return choose(language, "“${m.groupValues[1]}” and its bookmarks, notes, and reading progress will be permanently removed from this device.", "「${m.groupValues[1]}」とブックマーク、メモ、読書進捗を端末から完全に削除します。", "“${m.groupValues[1]}”及其书签、笔记和阅读进度将从设备中永久删除。", "‘${m.groupValues[1]}’과(와) 북마크, 노트 및 읽기 진행률이 기기에서 영구 삭제됩니다.")
        }
        match("Trích đoạn & Ghi chú \\(Trang (\\d+)\\)")?.let { m ->
            return choose(language, "Highlights and notes (Page ${m.groupValues[1]})", "ハイライトとメモ（${m.groupValues[1]} ページ）", "高亮与笔记（第 ${m.groupValues[1]} 页）", "하이라이트 및 노트 (${m.groupValues[1]}페이지)")
        }
        match("Trình đọc văn bản không hỗ trợ định dạng (.+)")?.let { m ->
            return choose(language, "The text reader does not support ${m.groupValues[1]}", "テキストリーダーは ${m.groupValues[1]} に対応していません", "文本阅读器不支持 ${m.groupValues[1]}", "텍스트 리더가 ${m.groupValues[1]} 형식을 지원하지 않습니다")
        }
        match("Xóa thẻ: #(.+)\\?")?.let { m ->
            return choose(language, "Delete tag #${m.groupValues[1]}?", "タグ #${m.groupValues[1]} を削除しますか？", "删除标签 #${m.groupValues[1]}？", "#${m.groupValues[1]} 태그를 삭제할까요?")
        }
        match("Đã thêm \"(.+)\" vào thư viện!")?.let { m ->
            val title = m.groupValues[1]
            return choose(language, "Added “$title” to your library!", "「$title」をライブラリに追加しました。", "已将《$title》添加到书库！", "‘$title’을(를) 라이브러리에 추가했습니다!")
        }
        match("(Dấu trang|Tô sáng|Ghi chú|Bộ sưu tập|Thẻ) \\((\\d+)\\)")?.let { m ->
            val label = byVietnamese[m.groupValues[1]]?.forLanguage(language) ?: m.groupValues[1]
            return "$label (${m.groupValues[2]})"
        }
        match("Cỡ chữ: (.+)")?.let { m ->
            return choose(language, "Text size: ${m.groupValues[1]}", "文字サイズ：${m.groupValues[1]}", "字号：${m.groupValues[1]}", "글자 크기: ${m.groupValues[1]}")
        }
        match("Giãn dòng: (.+)")?.let { m ->
            return choose(language, "Line spacing: ${m.groupValues[1]}", "行間：${m.groupValues[1]}", "行距：${m.groupValues[1]}", "줄 간격: ${m.groupValues[1]}")
        }
        match("Kết quả (.+)")?.let { m ->
            return choose(language, "Result ${m.groupValues[1]}", "結果 ${m.groupValues[1]}", "结果 ${m.groupValues[1]}", "결과 ${m.groupValues[1]}")
        }
        return null
    }

    private fun Entry.forLanguage(language: AppLanguage) = when (language) {
        AppLanguage.ENGLISH -> en
        AppLanguage.VIETNAMESE -> vi
        AppLanguage.JAPANESE -> ja
        AppLanguage.SIMPLIFIED_CHINESE -> zh
        AppLanguage.KOREAN -> ko
    }

    private fun choose(language: AppLanguage, en: String, ja: String, zh: String, ko: String) = when (language) {
        AppLanguage.ENGLISH -> en
        AppLanguage.JAPANESE -> ja
        AppLanguage.SIMPLIFIED_CHINESE -> zh
        AppLanguage.KOREAN -> ko
        AppLanguage.VIETNAMESE -> error("Vietnamese templates are returned before translation")
    }

    private fun e(vi: String, en: String, ja: String, zh: String, ko: String) = Entry(vi, en, ja, zh, ko)
}
