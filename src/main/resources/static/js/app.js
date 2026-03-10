/* ========================================
   トイレマップ - メインJS
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
                // 기본 위치(서울)로 화장실 로딩
                loadNearbyToilets(currentLat, currentLng, SEARCH_RADIUS);
            }
        );
    } else {
        loadNearbyToilets(currentLat, currentLng, SEARCH_RADIUS);
    }

    // 지도 이동 완료 시 화장실 다시 로딩
    naver.maps.Event.addListener(map, 'idle', function() {
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
            content: '<div style="width:32px;height:32px;background:#FF6B6B;border-radius:50% 50% 50% 0;transform:rotate(-45deg);display:flex;align-items:center;justify-content:center;box-shadow:0 2px 6px rgba(0,0,0,0.3);"><i class="fas fa-restroom" style="color:#fff;font-size:14px;transform:rotate(45deg);"></i></div>',
            anchor: new naver.maps.Point(16, 32)
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

// ========== 화장실 리스트 렌더링 ==========
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

    list.innerHTML = toilets.map(function(toilet) {
        var tags = '';
        if (toilet.is24hours) tags += '<span>24時間</span>';
        if (toilet.isWheelchair) tags += '<span>車椅子対応</span>';
        if (toilet.hasDiaper) tags += '<span>おむつ交換台</span>';
        if (toilet.hasEmergency) tags += '<span>非常ベル</span>';
        if (toilet.hasCctv) tags += '<span>CCTV</span>';

        return '<div class="toilet-card" onclick="focusToilet(' + toilet.id + ',' + toilet.latitude + ',' + toilet.longitude + ')">' +
            '<div class="toilet-card-icon"><i class="fas fa-restroom"></i></div>' +
            '<div class="toilet-card-info">' +
                '<div class="toilet-card-name">' + toilet.name + '</div>' +
                '<div class="toilet-card-distance">' +
                    '<i class="fas fa-map-marker-alt"></i> ' +
                    (toilet.distance ? Math.round(toilet.distance) + 'm · 徒歩' + Math.ceil(toilet.distance / 80) + '分' : toilet.address) +
                '</div>' +
                (tags ? '<div class="toilet-card-tags">' + tags + '</div>' : '') +
            '</div>' +
            (toilet.avgScore && toilet.avgScore > 0 ?
                '<div class="toilet-card-rating"><i class="fas fa-star"></i> ' + toilet.avgScore.toFixed(1) + '</div>'
                : '') +
        '</div>';
    }).join('');
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

// ========== 화장실 상세 페이지 ==========
function viewToiletDetail(id) {
    fetch('/api/toilets/' + id)
        .then(function(response) { return response.json(); })
        .then(function(response) {
            if (response.success) {
                showDetailSidebar(response.data);
            }
        })
        .catch(function(error) {
            console.error('Error loading detail:', error);
        });
}

// ========== 상세 사이드바 표시 ==========
function showDetailSidebar(toilet) {
    var list = document.getElementById('toiletList');

    var facilityItems = '';
    facilityItems += '<div class="detail-facility-item">' + (toilet.is24hours ? '✅' : '❌') + ' 24時間利用可能</div>';
    facilityItems += '<div class="detail-facility-item">' + (toilet.isWheelchair ? '✅' : '❌') + ' 車椅子対応</div>';
    facilityItems += '<div class="detail-facility-item">' + (toilet.hasDiaper ? '✅' : '❌') + ' おむつ交換台</div>';
    facilityItems += '<div class="detail-facility-item">' + (toilet.hasEmergency ? '✅' : '❌') + ' 非常ベル</div>';
    facilityItems += '<div class="detail-facility-item">' + (toilet.hasCctv ? '✅' : '❌') + ' CCTV設置</div>';

    var toiletCounts = '';
    if (toilet.maleToiletCount > 0 || toilet.maleUrinalCount > 0) {
        toiletCounts += '<div class="detail-count">🚹 男性用: 大便器 ' + (toilet.maleToiletCount || 0) + ' / 小便器 ' + (toilet.maleUrinalCount || 0) + '</div>';
    }
    if (toilet.femaleToiletCount > 0) {
        toiletCounts += '<div class="detail-count">🚺 女性用: 大便器 ' + (toilet.femaleToiletCount || 0) + '</div>';
    }

    list.innerHTML =
        '<div class="detail-card">' +
            '<div class="detail-header">' +
                '<button class="detail-back" onclick="backToList()"><i class="fas fa-arrow-left"></i></button>' +
                '<h3>' + toilet.name + '</h3>' +
            '</div>' +
            '<div class="detail-address"><i class="fas fa-map-marker-alt"></i> ' + (toilet.address || '') + '</div>' +
            (toilet.phone ? '<div class="detail-phone"><i class="fas fa-phone"></i> ' + toilet.phone + '</div>' : '') +
            (toilet.openHours ? '<div class="detail-hours"><i class="fas fa-clock"></i> ' + toilet.openHours + '</div>' : '') +
            '<div class="detail-section-title">施設情報</div>' +
            '<div class="detail-facilities">' + facilityItems + '</div>' +
            (toiletCounts ? '<div class="detail-section-title">個室数</div>' + toiletCounts : '') +
            (toilet.avgScore > 0 ? '<div class="detail-section-title">評価</div><div class="detail-score"><i class="fas fa-star" style="color:#FFD700;"></i> ' + toilet.avgScore.toFixed(1) + ' (' + toilet.reviewCount + '件のレビュー)</div>' : '') +
        '</div>';
}

// ========== 리스트로 돌아가기 ==========
function backToList() {
    if (currentFilter === 'all') {
        loadNearbyToilets(currentLat, currentLng, SEARCH_RADIUS);
    } else {
        loadFilteredToilets(currentLat, currentLng, SEARCH_RADIUS, currentFilter);
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
        clearTimeout(searchTimeout);
        var self = this;
        searchTimeout = setTimeout(function() {
            var keyword = self.value.trim();
            if (keyword.length >= 2) {
                searchToilets(keyword);
            } else if (keyword.length === 0) {
                // 검색어 지우면 주변 화장실 다시 로딩
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

                // 검색 결과가 있으면 첫 번째 결과로 지도 이동
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

                // 100m 반경으로 검색, 없으면 점점 넓혀서
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
                var nearest = response.data[0]; // 거리순 정렬이므로 첫번째가 가장 가까움
                clearMarkers();
                for (var i = 0; i < response.data.length; i++) {
                    addMarker(response.data[i]);
                }
                renderToiletList(response.data);

                // 가장 가까운 화장실로 이동 + 인포윈도우
                var pos = new naver.maps.LatLng(nearest.latitude, nearest.longitude);
                map.setCenter(pos);
                map.setZoom(17);

                // 첫번째 마커의 인포윈도우 열기
                setTimeout(function() {
                    if (markers.length > 0 && infoWindows.length > 0) {
                        infoWindows[0].open(map, markers[0]);
                        currentInfoWindow = infoWindows[0];
                    }
                }, 500);

                showToast('最も近いトイレ: ' + nearest.name + ' (' + Math.round(nearest.distance) + 'm)');
            } else if (radius < 2000) {
                // 반경 넓혀서 재검색
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

// ========== Google 로그인 (추후 구현) ==========
function googleLogin() {
    showToast('Googleログインは準備中です。');
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

    // 바텀시트 드래그 기능
    initBottomSheet();
});

// ========== 바텀시트 드래그 ==========
function initBottomSheet() {
    var sheet = document.getElementById('bottomSheet');
    var handle = document.getElementById('sheetHandle');
    if (!sheet || !handle) return;

    var startY, startHeight;
    var minHeight = 120;
    var maxHeight = window.innerHeight * 0.7;

    handle.addEventListener('touchstart', function(e) {
        startY = e.touches[0].clientY;
        startHeight = sheet.offsetHeight;
        sheet.style.transition = 'none';
    });

    handle.addEventListener('touchmove', function(e) {
        var deltaY = startY - e.touches[0].clientY;
        var newHeight = Math.min(maxHeight, Math.max(minHeight, startHeight + deltaY));
        sheet.style.height = newHeight + 'px';
    });

    handle.addEventListener('touchend', function() {
        sheet.style.transition = 'height 0.3s ease';
        var currentHeight = sheet.offsetHeight;
        // 스냅 포인트
        if (currentHeight > maxHeight * 0.6) {
            sheet.style.height = maxHeight + 'px';
        } else if (currentHeight > minHeight * 2) {
            sheet.style.height = '280px';
        } else {
            sheet.style.height = minHeight + 'px';
        }
    });
}
