/* ========================================
   トイレマップ - メインJS
======================================== */

// ========== 네이버맵 전역 변수 ==========
var map;          // 네이버맵 객체
var markers = []; // 마커 배열

// ========== 네이버맵 SDK 동적 로딩 ==========
function loadNaverMapSDK() {
    return new Promise(function(resolve, reject) {
        if (typeof NAVER_CLIENT_ID === 'undefined' || !NAVER_CLIENT_ID) {
            reject(new Error('Naver Client ID not found'));
            return;
        }
        var script = document.createElement('script');
        script.src = 'https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=' + NAVER_CLIENT_ID;
        script.onload = function() {
            resolve();
        };
        script.onerror = function() {
            reject(new Error('Failed to load Naver Maps script'));
        };
        document.head.appendChild(script);
    });
}

// ========== 네이버맵 초기화 ==========
function initMap() {
    var mapOptions = {
        center: new naver.maps.LatLng(37.5665, 126.9780),
        zoom: 15,
        zoomControl: true,
        zoomControlOptions: {
            position: naver.maps.Position.RIGHT_CENTER
        }
    };

    map = new naver.maps.Map('map', mapOptions);

    // 현재 위치로 자동 이동
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                var lat = position.coords.latitude;
                var lng = position.coords.longitude;
                var locPosition = new naver.maps.LatLng(lat, lng);
                map.setCenter(locPosition);
            }
        );
    }
}

// ========== 로그인 모달 ==========
function openLoginModal() {
    var modal = document.getElementById('loginModal');
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
}

function closeLoginModal(event) {
    if (event && event.target !== event.currentTarget) return;
    var modal = document.getElementById('loginModal');
    modal.classList.remove('show');
    document.body.style.overflow = '';
}

// ESC 키로 모달 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeLoginModal();
    }
});

// ========== 사용자 메뉴 드롭다운 ==========
function toggleUserMenu() {
    var menu = document.getElementById('userMenu');
    menu.classList.toggle('show');
}

// 메뉴 외부 클릭 시 닫기
document.addEventListener('click', function(e) {
    var menu = document.getElementById('userMenu');
    if (menu && !e.target.closest('.user-info')) {
        menu.classList.remove('show');
    }
});

// ========== 필터 태그 ==========
document.querySelectorAll('.filter-tag').forEach(function(tag) {
    tag.addEventListener('click', function() {
        document.querySelectorAll('.filter-tag').forEach(function(t) {
            t.classList.remove('active');
        });
        this.classList.add('active');

        var filter = this.dataset.filter;
        console.log('Filter selected:', filter);
        // TODO: 필터에 맞는 화장실 목록 불러오기
    });
});

// ========== 검색 ==========
var searchInput = document.getElementById('searchInput');
if (searchInput) {
    var searchTimeout;
    searchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        var self = this;
        searchTimeout = setTimeout(function() {
            var keyword = self.value.trim();
            if (keyword.length >= 2) {
                searchToilets(keyword);
            }
        }, 500);
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
            function(position) {
                var lat = position.coords.latitude;
                var lng = position.coords.longitude;
                console.log('Current location:', lat, lng);
                loadNearbyToilets(lat, lng, 500);
            },
            function(error) {
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
            function(position) {
                var lat = position.coords.latitude;
                var lng = position.coords.longitude;
                var moveLatLng = new naver.maps.LatLng(lat, lng);
                map.setCenter(moveLatLng);
            },
            function(error) {
                alert('位置情報の取得に失敗しました。');
            }
        );
    }
}

// ========== 주변 화장실 불러오기 ==========
function loadNearbyToilets(lat, lng, radius) {
    fetch('/api/toilets?lat=' + lat + '&lng=' + lng + '&radius=' + radius)
        .then(function(response) { return response.json(); })
        .then(function(response) {
            if (response.success) {
                renderToiletList(response.data);
            } else {
                console.error('API Error:', response.errorCode, response.message);
                showToast(response.message || 'エラーが発生しました。');
            }
        })
        .catch(function(error) {
            console.error('Error loading toilets:', error);
        });
}

// ========== 화장실 리스트 렌더링 ==========
function renderToiletList(toilets) {
    var list = document.getElementById('toiletList');

    if (!toilets || toilets.length === 0) {
        list.innerHTML =
            '<div class="toilet-list-empty">' +
                '<i class="fas fa-search"></i>' +
                '<p>周辺にトイレが見つかりませんでした</p>' +
            '</div>';
        return;
    }

    list.innerHTML = toilets.map(function(toilet) {
        return '<div class="toilet-card" onclick="viewToiletDetail(' + toilet.id + ')">' +
            '<div class="toilet-card-icon"><i class="fas fa-restroom"></i></div>' +
            '<div class="toilet-card-info">' +
                '<div class="toilet-card-name">' + toilet.name + '</div>' +
                '<div class="toilet-card-distance">' +
                    '<i class="fas fa-map-marker-alt"></i> ' +
                    (toilet.distance ? Math.round(toilet.distance) + 'm · 徒歩' + Math.ceil(toilet.distance / 80) + '分' : '') +
                '</div>' +
                '<div class="toilet-card-tags">' +
                    (toilet.is24hours ? '<span>24時間</span>' : '') +
                    (toilet.isWheelchair ? '<span>車椅子対応</span>' : '') +
                    (toilet.hasDiaper ? '<span>おむつ交換台</span>' : '') +
                    (toilet.hasPaper ? '<span>トイレットペーパー</span>' : '') +
                '</div>' +
            '</div>' +
            (toilet.avgScore ?
                '<div class="toilet-card-rating"><i class="fas fa-star"></i> ' + toilet.avgScore.toFixed(1) + '</div>'
                : '') +
        '</div>';
    }).join('');
}

// ========== 화장실 상세 페이지 ==========
function viewToiletDetail(id) {
    console.log('View toilet:', id);
    // TODO: 사이드바에 상세 정보 표시
}

// ========== Google 로그인 (추후 구현) ==========
function googleLogin() {
    alert('Googleログインは準備中です。');
}

// ========== 토스트 알림 ==========
function showToast(message) {
    var toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = '<i class="fas fa-check-circle"></i><span>' + message + '</span>';
    document.body.appendChild(toast);

    setTimeout(function() { toast.classList.add('show'); }, 100);
    setTimeout(function() {
        toast.classList.remove('show');
        setTimeout(function() { toast.remove(); }, 400);
    }, 3000);
}

// ========== 페이지 로드 시 ==========
document.addEventListener('DOMContentLoaded', function() {
    console.log('トイレマップ loaded!');

    // 네이버맵 SDK는 index.html에서 정적 script 태그로 로딩됨
    if (typeof naver !== 'undefined' && naver.maps) {
        console.log('Naver Maps SDK ready!');
        initMap();
    } else {
        console.error('Naver Maps SDK not loaded!');
    }
});
