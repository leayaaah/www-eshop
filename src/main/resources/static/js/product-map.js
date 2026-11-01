(function () {
    'use strict';

    const svg = document.getElementById('product-map');
    const legend = document.getElementById('product-map-legend');
    const summary = document.getElementById('product-map-summary');
    const details = document.getElementById('product-map-details');
    let ranking = document.getElementById('product-map-ranking');
    let categories = document.getElementById('product-map-categories');
    const search = document.getElementById('product-map-search');
    const metric = document.getElementById('product-map-metric');
    const days = document.getElementById('product-map-days');
    const mode = document.getElementById('product-map-mode');
    const colors = ['#2563eb', '#059669', '#d97706', '#db2777', '#7c3aed', '#0891b2', '#dc2626', '#65a30d'];
    let points = [];
    let heat = new Map();
    let activeCategories = new Set();
    let explainedVariance = 0;
    let heatData = null;
    let selectedPointId = null;

    Promise.all([load('/admin/products/map/data'), loadHeat()])
        .then(([data, heatData]) => {
            points = data.points || [];
            explainedVariance = data.explainedVariance || 0;
            applyHeat(heatData);
            activeCategories = new Set(points.map(point => point.category));
            renderLegend();
            render();
            renderRanking(heatData);
            renderCategories(heatData);
        })
        .catch(error => {
            summary.textContent = error.message;
            summary.classList.add('alert');
        });

    search.addEventListener('input', render);
    metric.addEventListener('change', refreshHeat);
    days.addEventListener('change', refreshHeat);
    mode.addEventListener('change', render);

    function load(url) {
        return fetch(url, {headers: {'Accept': 'application/json'}}).then(response => {
            if (!response.ok) throw new Error('Không thể tải dữ liệu bản đồ.');
            return response.json();
        });
    }

    function loadHeat() {
        return load(`/admin/products/map/heat?metric=${metric.value}&days=${days.value}`);
    }

    function refreshHeat() {
        summary.textContent = 'Đang tải số liệu nhiệt...';
        loadHeat().then(data => {
            applyHeat(data);
            render();
            renderRanking(data);
            renderCategories(data);
        }).catch(error => {
            summary.textContent = error.message;
            summary.classList.add('alert');
        });
    }

    function applyHeat(data) {
        heatData = data;
        heat = new Map((data.points || []).map(point => [point.productId, point]));
        summary.textContent = `${points.length} sản phẩm có vector · `
            + `Hai trục giải thích ${(explainedVariance * 100).toFixed(1)}% phương sai · `
            + `${data.days} ngày · ${metricLabel(data.metric)} · `
            + `${Math.round(data.views)} lượt xem, ${Math.round(data.carts)} thêm giỏ, ${Math.round(data.purchases)} sản phẩm đã mua`;
    }

    function renderLegend() {
        const categories = [...new Set(points.map(point => point.category))].sort();
        legend.replaceChildren();
        categories.forEach((category, index) => {
            const label = document.createElement('label');
            label.className = 'map-legend-item';
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.checked = true;
            checkbox.addEventListener('change', () => {
                checkbox.checked ? activeCategories.add(category) : activeCategories.delete(category);
                render();
            });
            const swatch = document.createElement('span');
            swatch.className = 'map-swatch';
            swatch.style.backgroundColor = colors[index % colors.length];
            label.append(checkbox, swatch, document.createTextNode(category));
            legend.append(label);
        });
    }

    function render() {
        svg.replaceChildren();
        svg.onclick = event => {
            if (event.target === svg) clearSelection();
        };
        const query = search.value.trim().toLocaleLowerCase();
        points.forEach((point, index) => {
            const heatPoint = heat.get(point.id) || {intensity: 0};
            const visible = activeCategories.has(point.category);
            const match = !query || point.name.toLocaleLowerCase().includes(query);
            const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.setAttribute('cx', 50 + point.x * 900);
            circle.setAttribute('cy', 630 - point.y * 580);
            const size = mode.value === 'opacity' ? 7 : 6 + heatPoint.intensity * 10;
            circle.setAttribute('r', match && query ? size + 3 : size);
            circle.setAttribute('fill', colors[categoryIndex(point.category) % colors.length]);
            circle.style.opacity = mode.value === 'size' ? '1' : String(.2 + heatPoint.intensity * .8);
            circle.classList.add('product-map-point');
            if (!visible) circle.classList.add('is-hidden');
            if (query && !match) circle.classList.add('is-dimmed');
            circle.setAttribute('tabindex', visible ? '0' : '-1');
            circle.setAttribute('aria-label', point.name);
            circle.addEventListener('mouseenter', event => showTooltip(event, point, heatPoint));
            circle.addEventListener('mouseleave', hideTooltip);
            circle.addEventListener('focus', event => showTooltip(event, point, heatPoint));
            circle.addEventListener('blur', hideTooltip);
            circle.addEventListener('click', event => {
                event.stopPropagation();
                selectedPointId === point.id ? clearSelection() : showDetails(point);
            });
            svg.append(circle);
        });
    }

    function categoryIndex(category) {
        return [...new Set(points.map(point => point.category))].sort().indexOf(category);
    }

    function showTooltip(event, point, heatPoint) {
        const title = document.createElement('title');
        title.textContent = `${point.name} · ${point.category} · ${formatPrice(point.price)}`
            + ` · xem ${Math.round(heatPoint.views || 0)} · giỏ ${Math.round(heatPoint.carts || 0)}`
            + ` · mua ${Math.round(heatPoint.purchases || 0)} · chuyển đổi ${formatPercent(heatPoint.conversion)}`;
        event.currentTarget.replaceChildren(title);
    }

    function hideTooltip(event) {
        event.currentTarget.replaceChildren();
    }

    function showDetails(point) {
        selectedPointId = point.id;
        details.replaceChildren();
        const back = document.createElement('button');
        back.className = 'button secondary map-back-button';
        back.type = 'button';
        back.textContent = '← Xem Top 5';
        back.addEventListener('click', clearSelection);
        const heading = document.createElement('h2');
        heading.textContent = point.name;
        const meta = document.createElement('p');
        const heatPoint = heat.get(point.id) || {};
        meta.textContent = `${point.category} · ${formatPrice(point.price)} · `
            + `xem ${Math.round(heatPoint.views || 0)} · giỏ ${Math.round(heatPoint.carts || 0)} · `
            + `mua ${Math.round(heatPoint.purchases || 0)} · chuyển đổi ${formatPercent(heatPoint.conversion)}`;
        const title = document.createElement('h3');
        title.textContent = '5 sản phẩm gần nhất';
        const list = document.createElement('ul');
        (point.neighbors || []).forEach(neighbor => {
            const item = document.createElement('li');
            const link = document.createElement('a');
            link.href = `/admin/products/${neighbor.id}/edit`;
            link.textContent = neighbor.name;
            item.append(link, document.createTextNode(` (${(neighbor.similarity * 100).toFixed(1)}%)`));
            list.append(item);
        });
        details.append(back, heading, meta, title, list);
    }

    function clearSelection() {
        selectedPointId = null;
        details.replaceChildren();
        const heading = document.createElement('h2');
        heading.textContent = 'Tổng quan nhiệt';
        const hint = document.createElement('p');
        hint.textContent = 'Chọn một điểm để xem các sản phẩm gần nhất.';
        ranking = document.createElement('div');
        ranking.id = 'product-map-ranking';
        categories = document.createElement('div');
        categories.id = 'product-map-categories';
        details.append(heading, hint, ranking, categories);
        if (heatData) {
            renderRanking(heatData);
            renderCategories(heatData);
        }
    }

    function formatPrice(price) {
        return new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(price);
    }

    function formatPercent(value) {
        return value && value >= 0.05 ? `${(value * 100).toFixed(1)}%` : 'chưa đủ 5 lượt xem';
    }

    function metricLabel(value) {
        return {INTEREST: 'điểm quan tâm', VIEWS: 'lượt xem', CARTS: 'thêm giỏ',
            PURCHASES: 'mua', CONVERSION: 'tỉ lệ chuyển đổi'}[value] || value;
    }

    function renderRanking(data) {
        const sorted = [...(data.points || [])].sort((a, b) => b.intensity - a.intensity);
        ranking.replaceChildren();
        [['Top 5 nóng nhất', sorted.slice(0, 5)], ['Top 5 nguội nhất', sorted.slice(-5).reverse()]]
            .forEach(([headingText, values]) => {
                const section = document.createElement('section');
                section.className = 'ranking';
                const heading = document.createElement('h3');
                heading.textContent = headingText;
                const list = document.createElement('ol');
                values.forEach(value => {
                    const item = document.createElement('li');
                    const product = points.find(point => point.id === value.productId);
                    const link = document.createElement('a');
                    link.href = '#';
                    link.textContent = product ? product.name : `Sản phẩm #${value.productId}`;
                    link.addEventListener('click', event => {
                        event.preventDefault();
                        const selected = points.find(point => point.id === value.productId);
                        if (selected) showDetails(selected);
                    });
                    item.append(link, document.createTextNode(
                        ` · ${metricLabel(data.metric)} ${value.raw.toFixed(2)}`));
                    list.append(item);
                });
                section.append(heading, list);
                ranking.append(section);
            });
    }

    function renderCategories(data) {
        // categories.replaceChildren();
        // const heading = document.createElement('h3');
        // heading.textContent = 'Tổng hợp theo danh mục';
        // categories.append(heading);
        // [...(data.categories || [])].sort((a, b) => b.intensity - a.intensity).forEach(category => {
        //     const row = document.createElement('div');
        //     row.className = 'heat-category';
        //     const label = document.createElement('span');
        //     label.textContent = category.category;
        //     const bar = document.createElement('span');
        //     bar.className = 'heat-category-bar';
        //     bar.style.width = `${Math.max(3, category.intensity * 100)}%`;
        //     row.append(label, bar);
        //     categories.append(row);
        // });
    }
})();
