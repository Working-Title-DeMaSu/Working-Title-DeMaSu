/* ========================================
   トイレマップ - メインJS
======================================== */

// ========== 로그인 모달 ==========
function openLoginModal() {
    const modal = document.getElementById('loginModal');
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
}

function closeLoginModal(event) {
    // overlay 클릭 시에만 닫기 (모달 내부 클릭은 무시)
    if (event && event.target !== event.currentTarget) return;
    const modal = document.getElementById('loginModal');
    modal.classList.remove('show');
    document.body.style.overflow = '';
}

// ESC 키로 모달 닫기
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closeLoginModal();
    }
});

// ========== 사용자 메뉴 드롭다운 ==========
function toggleUserMenu() {
    const menu = document.getElementById('userMenu');
    menu.classList.toggle('show');
}

// 메뉴 외부 클릭 시 닫기
document.addEventListener('click', (e) => {
    const menu = document.getElementById('userMenu');
    if (menu && !e.target.closest('.user-info')) {
        menu.classList.remove('show');
    }
});

// ========== 필터 태그 ==========
document.querySelectorAll('.filter-tag').forEach(tag => {
    tag.addEventListener('click', function() {
        // 모든 태그에서 active 제거
        document.querySelectorAll('.filter-tag').forEach(t => t.classList.remove('active'));
        // 클릭한 태그에 active 추가
        this.classList.add('active');

        const filter = this.dataset.filter;
        console.log('Filter selected:', filter);
        // TODO: 필터에 맞는 화장실 목록 불러오기
    });
});

// ========== 검색 ==========
const searchInput = document.getElementById('searchInput');
if (searchInput) {
    let searchTimeout;
    searchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            const keyword = this.value.trim();
            if (keyword.length >= 2) {
                searchToilets(keyword);
            }
        }, 500); // 500ms 디바운스
    });
}

function searchToilets(keyword) {
    console.log('Searching:', keyword);
    // TODO: API 호출 구현
}

// ========== 가장 가까운 화장실 ==========
function findNearest() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                console.log('Current location:', lat, lng);
                // TODO: 주변 화장실 API 호출
                loadNearbyToilets(lat, lng, 500);
            },
            (error) => {
                alert('位置情報の取得に失敗しました。位置情報の利用を許可してください。');
            }
        );
    } else {
        alert('このブラウザでは位置情報がサポートされていません。');
    }
}

// ========== 현재 위치로 이동 ==========
function moveToMyLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                console.log('Move to:', lat, lng);
                // TODO: 카카오맵 중심 이동
            },
            (error) => {
                alert('位置情報の取得に失敗しました。');
            }
        );
    }
}

// ========== 주변 화장실 불러오기 ==========
function loadNearbyToilets(lat, lng, radius) {
    fetch(`/api/toilets?lat=${lat}&lng=${lng}&radius=${radius}`)
        .then(response => response.json())
        .then(data => {
            renderToiletList(data);
        })
        .catch(error => {
            console.error('Error loading toilets:', error);
        });
}

// ========== 화장실 리스트 렌더링 ==========
function renderToiletList(toilets) {
    const list = document.getElementById('toiletList');

    if (!toilets || toilets.length === 0) {
        list.innerHTML = `
            <div class="toilet-list-empty">
                <i class="fas fa-search"></i>
                <p>周辺にトイレが見つかりませんでした</p>
            </div>
        `;
        return;
    }

    list.innerHTML = toilets.map(toilet => `
        <div class="toilet-card" onclick="viewToiletDetail(${toilet.id})">
            <div class="toilet-card-icon">
                <i class="fas fa-restroom"></i>
            </div>
            <div class="toilet-card-info">
                <div class="toilet-card-name">${toilet.name}</div>
                <div class="toilet-card-distance">
                    📍 ${toilet.distance ? Math.round(toilet.distance) + 'm' : ''}
                    ${toilet.distance ? ' · 徒歩' + Math.ceil(toilet.distance / 80) + '分' : ''}
                </div>
                <div class="toilet-card-tags">
                    ${toilet.is24hours ? '<span>24時間</span>' : ''}
                    ${toilet.isWheelchair ? '<span>車椅子対応</span>' : ''}
                    ${toilet.hasDiaper ? '<span>おむつ交換台</span>' : ''}
                    ${toilet.hasPaper ? '<span>トイレットペーパー</span>' : ''}
                </div>
            </div>
            ${toilet.avgScore ? `
                <div class="toilet-card-rating">
                    <i class="fas fa-star"></i> ${toilet.avgScore.toFixed(1)}
                </div>
            ` : ''}
        </div>
    `).join('');
}

// ========== 화장실 상세 페이지 ==========
function viewToiletDetail(id) {
    console.log('View toilet:', id);
    // TODO: 사이드바에 상세 정보 표시
}

// ========== Google 로그인 (추후 구현) ==========
function googleLogin() {
    alert('Googleログインは準備中です。');
    // TODO: Google OAuth 구현
}

// ========== 토스트 알림 ==========
function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = `<i class="fas fa-check-circle"></i><span>${message}</span>`;
    document.body.appendChild(toast);

    setTimeout(() => toast.classList.add('show'), 100);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 400);
    }, 3000);
}

// ========== 페이지 로드 시 ==========
document.addEventListener('DOMContentLoaded', () => {
    console.log('トイレマップ loaded!');

    // 자동 위치 검색 (선택사항)
    // findNearest();
});
