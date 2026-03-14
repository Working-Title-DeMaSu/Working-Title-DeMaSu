/* ============================================
   デマス 共通ヘッダー JS
   ============================================ */

// ログアウト後（?logout=true）はページロード時にモーダルを自動で開かない
// ただし、ユーザーが手動でログインボタンを押した場合はモーダルを開く
// → URLから ?logout=true を除去してからモーダルを表示する
if (window.location.search && window.location.search.includes('logout')) {
    window.openLoginModal = function() {
        // URLのクエリパラメータを除去してブラウザ履歴を更新（リロードなし）
        history.replaceState(null, '', window.location.pathname);
        // 元の openLoginModal を呼び出すため、app.js の実装を直接実行
        var modal = document.getElementById('loginModal');
        if (modal) {
            modal.style.display = 'flex';
            document.body.style.overflow = 'hidden';
        }
    };
}

// ======= ユーザードロップダウン =======
function toggleUserMenu(event) {
    event.stopPropagation();
    var dropdown = document.getElementById('userDropdown');
    var chevron = document.querySelector('.chevron-icon');
    if (!dropdown) return;
    var isOpen = dropdown.classList.contains('open');
    if (isOpen) {
        dropdown.classList.remove('open');
        if (chevron) chevron.style.transform = 'rotate(0deg)';
    } else {
        dropdown.classList.add('open');
        if (chevron) chevron.style.transform = 'rotate(180deg)';
    }
}

// 外部クリックでドロップダウンを閉じる
document.addEventListener('click', function(e) {
    var wrapper = document.getElementById('userMenuWrapper');
    if (wrapper && !wrapper.contains(e.target)) {
        var dropdown = document.getElementById('userDropdown');
        var chevron = document.querySelector('.chevron-icon');
        if (dropdown) dropdown.classList.remove('open');
        if (chevron) chevron.style.transform = 'rotate(0deg)';
    }
});

// ======= トースト通知 =======
function showToast(msg, type) {
    var toast = document.getElementById('dmsToast');
    if (!toast || !msg) return;
    toast.textContent = msg;
    toast.className = 'dms-toast ' + (type || 'success');
    void toast.offsetWidth; // reflow for animation reset
    toast.classList.add('show');
    setTimeout(function() {
        toast.classList.remove('show');
    }, 3000);
}

// ======= トースト初期化 (body末尾で実行するのでDOM準備済み) =======
(function() {
    var msg = '';
    var type = 'success';
    var toastType = window._toastType || '';

    if (toastType === 'logout') {
        msg = 'ログアウトしました';
    } else if (toastType === 'register') {
        msg = '会員登録が完了しました！';
    } else if (toastType === 'loginError') {
        sessionStorage.removeItem('justLoggedIn');
        msg = 'IDまたはパスワードが正しくありません';
        type = 'error';
    } else if (sessionStorage.getItem('justLoggedIn')) {
        sessionStorage.removeItem('justLoggedIn');
        msg = 'ログインしました';
    }

    if (msg) {
        setTimeout(function() { showToast(msg, type); }, 300);
    }
})();
