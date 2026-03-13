/* ========================================
   トイレマップ - メインJS
   Sidebar Layout + Detail View + Reviews
======================================== */

// ========== 전역 변수 ==========
var map;              // 네이버맵 객체
var markers = [];     // 마커 배열
var infoWindows = []; // 인포윈도우 배열
var currentInfoWindow = null; // 현재 열려있는 인포윈도우
var currentLat = 37.5665;  // 현재 위도 (기본값: 서울)
var currentLng = 126.9780; // 현재 경도
var currentFilter = 'all'; // 현재 필터
var myLocationMarker = null; // 내 위치 마커
var SEARCH_RADIUS = 800;     // 검색 반경 (800m 고정)
var currentPage = 1;         // current page for pagination
var ITEMS_PER_PAGE = 4;      // items per page
var allToilets = [];         // store all toilets for pagination
var isDetailView = false;    // 상세보기 모드 여부

// ========== 네이버맵 초기화 ==========
function initMap() {
    var mapOptions = {
        center: new naver.maps.LatLng(currentLat, currentLng),
        zoom: 15,
        zoomControl: true,
        zoomControlOptions: {
            position: naver.maps.Position.RIGHT_CENTER
        }
    };

    map = new naver.maps.Map('map', mapOptions);

    // 현재 위치로 자동 이동 + 주변 화장실 로딩
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                currentLat = position.coords.latitude;
                currentLng = position.coords.longitude;
                var locPosition = new naver.maps.LatLng(currentLat, currentLng);
                map.setCenter(locPosition);

                // 내 위치 마커 표시
                showMyLocationMarker(currentLat, currentLng);

                // 주변 화장실 로딩
                loadNearbyToilets(currentLat, currentLng, SEARCH_RADIUS);
            },
            function(error) {
                console.log('位置情報の取得に失敗:', error);
                loadNearbyToilets(currentLat, currentLng, SEARCH_RADIUS);
            }
        );
    } else {
        loadNearbyToilets(currentLat, currentLng, SEARCH_RADIUS);
    }

    // 지도 이동 완료 시 화장실 다시 로딩 (상세보기 중엔 무시)
    naver.maps.Event.addListener(map, 'idle', function() {
        if (isDetailView) return;
        var center = map.getCenter();
        currentLat = center.lat();
        currentLng = center.lng();
        var radius = SEARCH_RADIUS;
        if (currentFilter === 'all') {
            loadNearbyToilets(currentLat, currentLng, radius);
        } else {
            loadFilteredToilets(currentLat, currentLng, radius, currentFilter);
        }
    });
}

// ========== 내 위치 마커 ==========
function showMyLocationMarker(lat, lng) {
    if (myLocationMarker) {
        myLocationMarker.setMap(null);
    }
    myLocationMarker = new naver.maps.Marker({
        position: new naver.maps.LatLng(lat, lng),
        map: map,
        icon: {
            content: '<div style="width:16px;height:16px;background:#4285F4;border:3px solid #fff;border-radius:50%;box-shadow:0 0 6px rgba(66,133,244,0.5);"></div>',
            anchor: new naver.maps.Point(11, 11)
        },
        zIndex: 999
    });
}

// ========== 마커 전체 삭제 ==========
function clearMarkers() {
    for (var i = 0; i < markers.length; i++) {
        markers[i].setMap(null);
    }
    markers = [];
    for (var j = 0; j < infoWindows.length; j++) {
        infoWindows[j].close();
    }
    infoWindows = [];
    currentInfoWindow = null;
}

// ========== 마커 생성 ==========
function addMarker(toilet) {
    if (!toilet.latitude || !toilet.longitude) return;

    var marker = new naver.maps.Marker({
        position: new naver.maps.LatLng(toilet.latitude, toilet.longitude),
        map: map,
        icon: {
            content: '<div style="width:28px;height:28px;background:#00C896;border-radius:50%;border:3px solid #fff;display:flex;align-items:center;justify-content:center;box-shadow:0 2px 6px rgba(0,0,0,0.25);"><i class="fas fa-restroom" style="color:#fff;font-size:11px;"></i></div>',
            anchor: new naver.maps.Point(14, 14)
        }
    });

    // 인포윈도우 내용
    var tags = '';
    if (toilet.is24hours) tags += '<span class="iw-tag">24時間</span>';
    if (toilet.isWheelchair) tags += '<span class="iw-tag">車椅子</span>';
    if (toilet.hasDiaper) tags += '<span class="iw-tag">おむつ</span>';
    if (toilet.hasEmergency) tags += '<span class="iw-tag">非常ベル</span>';
    if (toilet.hasCctv) tags += '<span class="iw-tag">CCTV</span>';

    var distanceText = '';
    if (toilet.distance) {
        distanceText = Math.round(toilet.distance) + 'm · 徒歩' + Math.ceil(toilet.distance / 80) + '分';
    }

    var ratingText = '';
    if (toilet.avgScore && toilet.avgScore > 0) {
        ratingText = '<div class="iw-rating"><i class="fas fa-star" style="color:#FFD700;"></i> ' + toilet.avgScore.toFixed(1) + '</div>';
    }

    var infoWindow = new naver.maps.InfoWindow({
        content: '<div class="info-window">' +
            '<div class="iw-title">' + toilet.name + '</div>' +
            '<div class="iw-address"><i class="fas fa-map-marker-alt"></i> ' + (toilet.address || '') + '</div>' +
            (distanceText ? '<div class="iw-distance"><i class="fas fa-walking"></i> ' + distanceText + '</div>' : '') +
            ratingText +
            (tags ? '<div class="iw-tags">' + tags + '</div>' : '') +
            (toilet.phone ? '<div class="iw-phone"><i class="fas fa-phone"></i> ' + toilet.phone + '</div>' : '') +
            '<div class="iw-detail"><a href="javascript:viewToiletDetail(' + toilet.id + ')">詳しく見る →</a></div>' +
            '</div>',
        borderWidth: 0,
        backgroundColor: 'transparent',
        disableAnchor: true,
        pixelOffset: new naver.maps.Point(0, -10)
    });

    // 마커 클릭 이벤트
    naver.maps.Event.addListener(marker, 'click', function() {
        if (currentInfoWindow) {
            currentInfoWindow.close();
        }
        if (currentInfoWindow === infoWindow) {
            currentInfoWindow = null;
            return;
        }
        infoWindow.open(map, marker);
        currentInfoWindow = infoWindow;
    });

    markers.push(marker);
    infoWindows.push(infoWindow);
}

// ========== 주변 화장실 불러오기 ==========
function loadNearbyToilets(lat, lng, radius) {
    fetch('/api/toilets?lat=' + lat + '&lng=' + lng + '&radius=' + radius)
        .then(function(response) { return response.json(); })
        .then(function(response) {
            if (response.success) {
                clearMarkers();
                var toilets = response.data;
                for (var i = 0; i < toilets.length; i++) {
                    addMarker(toilets[i]);
                }
                renderToiletList(toilets);
            } else {
                console.error('API Error:', response.errorCode, response.message);
            }
        })
        .catch(function(error) {
            console.error('Error loading toilets:', error);
        });
}

// ========== 필터 검색 ==========
function loadFilteredToilets(lat, lng, radius, filter) {
    var url = '/api/toilets/filter?lat=' + lat + '&lng=' + lng + '&radius=' + radius;
    if (filter === 'is24hours') url += '&is24hours=1';
    if (filter === 'isWheelchair') url += '&isWheelchair=1';
    if (filter === 'hasDiaper') url += '&hasDiaper=1';
    if (filter === 'hasPaper') url += '&hasPaper=1';

    fetch(url)
        .then(function(response) { return response.json(); })
        .then(function(response) {
            if (response.success) {
                clearMarkers();
                var toilets = response.data;
                for (var i = 0; i < toilets.length; i++) {
                    addMarker(toilets[i]);
                }
                renderToiletList(toilets);
            } else {
                console.error('API Error:', response.errorCode, response.message);
            }
        })
        .catch(function(error) {
            console.error('Error filtering toilets:', error);
        });
}

// ========== Toilet list rendering (with pagination) ==========
function renderToiletList(toilets) {
    var list = document.getElementById('toiletList');

    if (!toilets || toilets.length === 0) {
        list.innerHTML =
            '<div class="toilet-list-empty">' +
                '<i class="fas fa-search"></i>' +
                '<p>周辺にトイレが見つかりませんでした</p>' +
                '<p style="font-size:12px;color:#999;">地図を移動して探してみてください</p>' +
            '</div>';
        return;
    }

    // Store all toilets and reset to page 1
    allToilets = toilets;
    currentPage = 1;
    renderPage();
}

// Render current page
function renderPage() {
    var list = document.getElementById('toiletList');
    var totalPages = Math.ceil(allToilets.length / ITEMS_PER_PAGE);
    var start = (currentPage - 1) * ITEMS_PER_PAGE;
    var end = start + ITEMS_PER_PAGE;
    var pageToilets = allToilets.slice(start, end);

    var cardsHtml = pageToilets.map(function(toilet) {
        var tags = '';
        if (toilet.is24hours) tags += '<span>#24時間</span>';
        if (toilet.isWheelchair) tags += '<span>#車椅子対応</span>';
        if (toilet.hasDiaper) tags += '<span>#おむつ交換台</span>';
        if (toilet.hasEmergency) tags += '<span>#非常ベル</span>';
        if (toilet.hasCctv) tags += '<span>#CCTV</span>';

        var distanceHtml = '';
        if (toilet.distance) {
            var distKm = toilet.distance >= 1000 ?
                (toilet.distance / 1000).toFixed(1) + 'km' :
                Math.round(toilet.distance) + 'm';
            distanceHtml = '<i class="fas fa-walking"></i> ' + distKm;
        }

        var metaHtml = '';
        if (toilet.avgScore && toilet.avgScore > 0) {
            metaHtml += '<span class="toilet-card-rating"><i class="fas fa-star"></i> ' + toilet.avgScore.toFixed(1) + '</span>';
            if (toilet.reviewCount) {
                metaHtml += '<span class="toilet-card-review-count">(' + toilet.reviewCount + ')</span>';
            }
        }
        if (toilet.is24hours) {
            metaHtml += '<span class="toilet-card-hours"><i class="far fa-clock"></i> 24時間</span>';
        }

        return '<div class="toilet-card" onclick="focusToilet(' + toilet.id + ',' + toilet.latitude + ',' + toilet.longitude + ')">' +
            '<div class="toilet-card-header">' +
                '<div class="toilet-card-name">' + toilet.name + '</div>' +
                (distanceHtml ? '<div class="toilet-card-distance">' + distanceHtml + '</div>' : '') +
            '</div>' +
            '<div class="toilet-card-address">' + (toilet.address || '') + '</div>' +
            (metaHtml ? '<div class="toilet-card-meta">' + metaHtml + '</div>' : '') +
            (tags ? '<div class="toilet-card-tags">' + tags + '</div>' : '') +
            '<div class="toilet-card-detail-link"><i class="fas fa-lock"></i> 詳細情報を見るにはログインが必要です →</div>' +
        '</div>';
    }).join('');

    // Pagination controls
    var paginationHtml = '';
    if (totalPages > 1) {
        paginationHtml = '<div class="pagination">';
        paginationHtml += '<button class="page-btn" onclick="goToPage(' + (currentPage - 1) + ')" ' + (currentPage === 1 ? 'disabled' : '') + '>&lt;</button>';

        for (var i = 1; i <= totalPages; i++) {
            if (i === currentPage) {
                paginationHtml += '<button class="page-btn active">' + i + '</button>';
            } else if (i === 1 || i === totalPages || (i >= currentPage - 1 && i <= currentPage + 1)) {
                paginationHtml += '<button class="page-btn" onclick="goToPage(' + i + ')">' + i + '</button>';
            } else if (i === currentPage - 2 || i === currentPage + 2) {
                paginationHtml += '<span class="page-dots">...</span>';
            }
        }

        paginationHtml += '<button class="page-btn" onclick="goToPage(' + (currentPage + 1) + ')" ' + (currentPage === totalPages ? 'disabled' : '') + '>&gt;</button>';
        paginationHtml += '<span class="page-info">' + allToilets.length + '件中 ' + (start + 1) + '-' + Math.min(end, allToilets.length) + '件</span>';
        paginationHtml += '</div>';
    }

    list.innerHTML = cardsHtml + paginationHtml;

    // Scroll to top of list
    list.scrollTop = 0;
}

// Go to specific page
function goToPage(page) {
    var totalPages = Math.ceil(allToilets.length / ITEMS_PER_PAGE);
    if (page < 1 || page > totalPages) return;
    currentPage = page;
    renderPage();
}

// ========== 카드 클릭 → 지도 이동 + 인포윈도우 ==========
function focusToilet(id, lat, lng) {
    var position = new naver.maps.LatLng(lat, lng);
    map.panTo(position);

    // 해당 마커의 인포윈도우 열기
    for (var i = 0; i < markers.length; i++) {
        var markerPos = markers[i].getPosition();
        if (Math.abs(markerPos.lat() - lat) < 0.0001 && Math.abs(markerPos.lng() - lng) < 0.0001) {
            if (currentInfoWindow) currentInfoWindow.close();
            infoWindows[i].open(map, markers[i]);
            currentInfoWindow = infoWindows[i];
            break;
        }
    }
}

// ========== 화장실 상세보기 (3개 API 병렬 호출) ==========
function viewToiletDetail(id) {
    var list = document.getElementById('toiletList');
    isDetailView = true;

    // 인포윈도우 닫기
    if (currentInfoWindow) {
        currentInfoWindow.close();
        currentInfoWindow = null;
    }

    // 로딩 표시
    list.innerHTML =
        '<div class="toilet-list-empty">' +
            '<i class="fas fa-spinner fa-spin"></i>' +
            '<p>読み込み中...</p>' +
        '</div>';

    // 3개 API 병렬 호출
    Promise.all([
        fetch('/api/toilets/' + id).then(function(r) { return r.json(); }),
        fetch('/api/toilets/' + id + '/tag').then(function(r) { return r.json(); }),
        fetch('/review/api/toilet/' + id).then(function(r) { return r.json(); })
    ])
    .then(function(results) {
        var toiletRes = results[0];
        var tagsRes = results[1];
        var reviewsRes = results[2];

        if (toiletRes.success) {
            var toilet = toiletRes.data;
            var tags = (tagsRes.success && tagsRes.data) ? tagsRes.data : [];
            var reviews = (reviewsRes.success && reviewsRes.data) ? reviewsRes.data : [];
            showDetailSidebar(toilet, tags, reviews);
        }
    })
    .catch(function(error) {
        console.error('Error loading detail:', error);
        list.innerHTML =
            '<div class="toilet-list-empty">' +
                '<i class="fas fa-exclamation-circle"></i>' +
                '<p>読み込みに失敗しました</p>' +
            '</div>';
    });
}

// ========== 상세 사이드바 표시 ==========
function showDetailSidebar(toilet, tags, reviews) {
    var list = document.getElementById('toiletList');

    // --- 시설 정보 ---
    var facilityItems = '';
    facilityItems += '<div class="detail-facility-item">' + (toilet.is24hours ? '<i class="fas fa-check-circle detail-yes"></i>' : '<i class="fas fa-times-circle detail-no"></i>') + ' 24時間利用可能</div>';
    facilityItems += '<div class="detail-facility-item">' + (toilet.isWheelchair ? '<i class="fas fa-check-circle detail-yes"></i>' : '<i class="fas fa-times-circle detail-no"></i>') + ' 車椅子対応</div>';
    facilityItems += '<div class="detail-facility-item">' + (toilet.hasDiaper ? '<i class="fas fa-check-circle detail-yes"></i>' : '<i class="fas fa-times-circle detail-no"></i>') + ' おむつ交換台</div>';
    facilityItems += '<div class="detail-facility-item">' + (toilet.hasEmergency ? '<i class="fas fa-check-circle detail-yes"></i>' : '<i class="fas fa-times-circle detail-no"></i>') + ' 非常ベル</div>';
    facilityItems += '<div class="detail-facility-item">' + (toilet.hasCctv ? '<i class="fas fa-check-circle detail-yes"></i>' : '<i class="fas fa-times-circle detail-no"></i>') + ' CCTV設置</div>';

    // --- 개실수 ---
    var toiletCounts = '';
    if (toilet.maleToiletCount > 0 || toilet.maleUrinalCount > 0) {
        toiletCounts += '<div class="detail-count"><i class="fas fa-male"></i> 男性用: 大便器 ' + (toilet.maleToiletCount || 0) + ' / 小便器 ' + (toilet.maleUrinalCount || 0) + '</div>';
    }
    if (toilet.femaleToiletCount > 0) {
        toiletCounts += '<div class="detail-count"><i class="fas fa-female"></i> 女性用: 大便器 ' + (toilet.femaleToiletCount || 0) + '</div>';
    }

    // --- 태그 ---
    var tagsHtml = '';
    if (tags && tags.length > 0) {
        tagsHtml = '<div class="detail-section-title">タグ</div><div class="detail-tags">';
        for (var t = 0; t < tags.length; t++) {
            tagsHtml += '<span class="detail-tag">' + escapeHtml(tags[t].tagName) + '</span>';
        }
        tagsHtml += '</div>';
    }

    // --- 별점 ---
    var ratingHtml = '';
    if (toilet.avgScore > 0) {
        ratingHtml = '<div class="detail-section-title">評価</div>' +
            '<div class="detail-rating-summary">' +
                '<div class="detail-rating-stars">' + generateStars(toilet.avgScore) + '</div>' +
                '<span class="detail-rating-number">' + toilet.avgScore.toFixed(1) + '</span>' +
                '<span class="detail-rating-count">(' + (toilet.reviewCount || 0) + '件)</span>' +
            '</div>';
    }

    // --- 리뷰 작성 버튼 ---
    var writeReviewBtn =
        '<div class="detail-write-review">' +
            '<a href="/review/write?toiletId=' + toilet.id + '" class="btn-write-review">' +
                '<i class="fas fa-pen"></i> レビューを書く' +
            '</a>' +
        '</div>';

    // --- 리뷰 목록 ---
    var reviewsHtml = '';
    reviewsHtml += '<div class="detail-section-title">レビュー' +
        (reviews.length > 0 ? ' (' + reviews.length + '件)' : '') +
        '</div>';

    if (!reviews || reviews.length === 0) {
        reviewsHtml += '<div class="detail-reviews-empty">' +
            '<i class="fas fa-comment-slash"></i>' +
            '<p>まだレビューがありません</p>' +
            '<p class="detail-reviews-empty-sub">最初のレビューを書いてみませんか？</p>' +
        '</div>';
    } else {
        reviewsHtml += '<div class="detail-reviews-list">';
        for (var r = 0; r < reviews.length; r++) {
            reviewsHtml += renderReviewCard(reviews[r]);
        }
        reviewsHtml += '</div>';
    }

    // --- 전체 조립 ---
    list.innerHTML =
        '<div class="detail-card">' +
            '<div class="detail-header">' +
                '<button class="detail-back" onclick="backToList()"><i class="fas fa-arrow-left"></i></button>' +
                '<h3>' + escapeHtml(toilet.name) + '</h3>' +
            '</div>' +
            '<div class="detail-address"><i class="fas fa-map-marker-alt"></i> ' + escapeHtml(toilet.address || '') + '</div>' +
            (toilet.phone ? '<div class="detail-phone"><i class="fas fa-phone"></i> ' + escapeHtml(toilet.phone) + '</div>' : '') +
            (toilet.openHours ? '<div class="detail-hours"><i class="fas fa-clock"></i> ' + escapeHtml(toilet.openHours) + '</div>' : '') +
            tagsHtml +
            '<div class="detail-section-title">施設情報</div>' +
            '<div class="detail-facilities">' + facilityItems + '</div>' +
            (toiletCounts ? '<div class="detail-section-title">個室数</div>' + toiletCounts : '') +
            ratingHtml +
            writeReviewBtn +
            reviewsHtml +
        '</div>';

    // 스크롤 맨 위로
    list.scrollTop = 0;
}

// ========== 별점 생성 ==========
function generateStars(score) {
    var html = '';
    var fullStars = Math.floor(score);
    var hasHalf = (score - fullStars) >= 0.5;
    for (var i = 0; i < fullStars; i++) {
        html += '<i class="fas fa-star"></i>';
    }
    if (hasHalf) {
        html += '<i class="fas fa-star-half-alt"></i>';
        fullStars++;
    }
    for (var j = fullStars; j < 5; j++) {
        html += '<i class="far fa-star"></i>';
    }
    return html;
}

// ========== 리뷰 카드 렌더링 ==========
function renderReviewCard(review) {
    var scoreNum = parseInt(review.cleanScore) || 0;

    // 날짜 포맷
    var dateStr = '';
    if (review.createdAt) {
        var d = new Date(review.createdAt);
        if (!isNaN(d.getTime())) {
            dateStr = d.getFullYear() + '.' +
                String(d.getMonth() + 1).padStart(2, '0') + '.' +
                String(d.getDate()).padStart(2, '0');
        }
    }

    // 리뷰 이미지
    var imagesHtml = '';
    if (review.images && review.images.length > 0) {
        imagesHtml = '<div class="review-images">';
        for (var i = 0; i < review.images.length; i++) {
            imagesHtml += '<img src="' + review.images[i].imageUrl + '" ' +
                'alt="Review" class="review-thumb" ' +
                'onclick="openLightbox(\'' + review.images[i].imageUrl + '\')">';
        }
        imagesHtml += '</div>';
    }

    // 프로필 아이콘
    var iconSrc = review.iconUrl || '/img/default.png';

    return '<div class="review-card">' +
        '<div class="review-card-header">' +
            '<img src="' + iconSrc + '" alt="avatar" class="review-avatar" onerror="this.src=\'/img/default.png\'">' +
            '<div class="review-author-info">' +
                '<span class="review-nickname">' + escapeHtml(review.nickname || '匿名') + '</span>' +
                '<span class="review-date">' + dateStr + '</span>' +
            '</div>' +
            '<div class="review-card-stars">' + generateStars(scoreNum) + '</div>' +
        '</div>' +
        (review.content ? '<div class="review-content">' + escapeHtml(review.content) + '</div>' : '') +
        imagesHtml +
    '</div>';
}

// ========== HTML 이스케이프 (XSS 방지) ==========
function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== 이미지 라이트박스 ==========
function openLightbox(imageUrl) {
    var overlay = document.getElementById('lightboxOverlay');
    var img = document.getElementById('lightboxImage');
    img.src = imageUrl;
    overlay.classList.add('show');
    document.body.style.overflow = 'hidden';
}

function closeLightbox(event) {
    if (event && event.target !== event.currentTarget &&
        !event.target.closest('.lightbox-close')) return;
    var overlay = document.getElementById('lightboxOverlay');
    overlay.classList.remove('show');
    document.body.style.overflow = '';
}

// ========== 리스트로 돌아가기 ==========
function backToList() {
    isDetailView = false;
    if (currentFilter === 'all') {
        loadNearbyToilets(currentLat, currentLng, SEARCH_RADIUS);
    } else {
        loadFilteredToilets(currentLat, currentLng, SEARCH_RADIUS, currentFilter);
    }
}

// ========== 로그인 모달 ==========
function openLoginModal() {
    document.getElementById('loginModal').style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function closeLoginModal() {
    document.getElementById('loginModal').style.display = 'none';
    document.body.style.overflow = '';
}

// ESC 키로 모달/라이트박스 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeLoginModal();
        closeLightbox();
    }
});

// 모달 배경 클릭 시 닫기
document.addEventListener('click', function(e) {
    var modal = document.getElementById('loginModal');
    if (modal && e.target === modal) {
        closeLoginModal();
    }
});

// ========== 필터 태그 ==========
document.querySelectorAll('.filter-tag').forEach(function(tag) {
    tag.addEventListener('click', function() {
        if (isDetailView) return; // 상세보기 중엔 필터 무시
        document.querySelectorAll('.filter-tag').forEach(function(t) {
            t.classList.remove('active');
        });
        this.classList.add('active');

        currentFilter = this.dataset.filter;
        var radius = SEARCH_RADIUS;

        if (currentFilter === 'all') {
            loadNearbyToilets(currentLat, currentLng, radius);
        } else {
            loadFilteredToilets(currentLat, currentLng, radius, currentFilter);
        }
    });
});

// ========== 검색 ==========
var searchInput = document.getElementById('searchInput');
if (searchInput) {
    var searchTimeout;
    searchInput.addEventListener('input', function() {
        if (isDetailView) {
            backToList();
        }
        clearTimeout(searchTimeout);
        var self = this;
        searchTimeout = setTimeout(function() {
            var keyword = self.value.trim();
            if (keyword.length >= 2) {
                searchToilets(keyword);
            } else if (keyword.length === 0) {
                loadNearbyToilets(currentLat, currentLng, SEARCH_RADIUS);
            }
        }, 500);
    });
}

function searchToilets(keyword) {
    fetch('/api/toilets/search?keyword=' + encodeURIComponent(keyword))
        .then(function(response) { return response.json(); })
        .then(function(response) {
            if (response.success) {
                clearMarkers();
                var toilets = response.data;
                for (var i = 0; i < toilets.length; i++) {
                    addMarker(toilets[i]);
                }
                renderToiletList(toilets);

                if (toilets.length > 0 && toilets[0].latitude && toilets[0].longitude) {
                    map.setCenter(new naver.maps.LatLng(toilets[0].latitude, toilets[0].longitude));
                }
            }
        })
        .catch(function(error) {
            console.error('Error searching:', error);
        });
}

// ========== 가장 가까운 화장실 ==========
function findNearest() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                var lat = position.coords.latitude;
                var lng = position.coords.longitude;
                currentLat = lat;
                currentLng = lng;
                showMyLocationMarker(lat, lng);
                isDetailView = false;
                findNearestInRadius(lat, lng, 100);
            },
            function(error) {
                showToast('位置情報の取得に失敗しました。位置情報の利用を許可してください。');
            }
        );
    } else {
        showToast('このブラウザでは位置情報がサポートされていません。');
    }
}

function findNearestInRadius(lat, lng, radius) {
    fetch('/api/toilets?lat=' + lat + '&lng=' + lng + '&radius=' + radius)
        .then(function(response) { return response.json(); })
        .then(function(response) {
            if (response.success && response.data.length > 0) {
                var nearest = response.data[0];
                clearMarkers();
                for (var i = 0; i < response.data.length; i++) {
                    addMarker(response.data[i]);
                }
                renderToiletList(response.data);

                var pos = new naver.maps.LatLng(nearest.latitude, nearest.longitude);
                map.setCenter(pos);
                map.setZoom(17);

                setTimeout(function() {
                    if (markers.length > 0 && infoWindows.length > 0) {
                        infoWindows[0].open(map, markers[0]);
                        currentInfoWindow = infoWindows[0];
                    }
                }, 500);

                showToast('最も近いトイレ: ' + nearest.name + ' (' + Math.round(nearest.distance) + 'm)');
            } else if (radius < 2000) {
                findNearestInRadius(lat, lng, radius + 200);
            } else {
                showToast('2km以内にトイレが見つかりませんでした。');
            }
        })
        .catch(function(error) {
            console.error('Error finding nearest:', error);
        });
}

// ========== 현재 위치로 이동 ==========
function moveToMyLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                currentLat = position.coords.latitude;
                currentLng = position.coords.longitude;
                var moveLatLng = new naver.maps.LatLng(currentLat, currentLng);
                map.setCenter(moveLatLng);
                showMyLocationMarker(currentLat, currentLng);
            },
            function(error) {
                showToast('位置情報の取得に失敗しました。');
            }
        );
    }
}

// ========== Google OAuth2 로그인 ==========
function googleLogin() {
    window.location.href = '/oauth2/authorization/google';
}

// ========== 토스트 알림 ==========
function showToast(message) {
    var toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = '<i class="fas fa-info-circle"></i><span>' + message + '</span>';
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

    if (typeof naver !== 'undefined' && naver.maps) {
        console.log('Naver Maps SDK ready!');
        initMap();
    } else {
        console.error('Naver Maps SDK not loaded!');
    }
});
