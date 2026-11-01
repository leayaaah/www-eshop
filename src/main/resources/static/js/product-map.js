(function () {
    'use strict';

    const svg = document.getElementById('product-map');
    const legend = document.getElementById('product-map-legend');
    const summary = document.getElementById('product-map-summary');
    const details = document.getElementById('product-map-details');
    const search = document.getElementById('product-map-search');
    const colors = ['#2563eb', '#059669', '#d97706', '#db2777', '#7c3aed', '#0891b2', '#dc2626', '#65a30d'];
    let points = [];
    let activeCategories = new Set();

    fetch('/admin/products/map/data', {headers: {'Accept': 'application/json'}})
        .then(response => {
            if (!response.ok) throw new Error('Không thể tải dữ liệu bản đồ.');
            return response.json();
        })
        .then(data => {
            points = data.points || [];
            activeCategories = new Set(points.map(point => point.category));
            const missing = (data.missingVectors || []).length;
            summary.textContent = `${data.productCount} sản phẩm có vector · `
                + `Hai trục giải thích ${(data.explainedVariance * 100).toFixed(1)}% phương sai`
                + (missing ? ` · ${missing} sản phẩm chưa có vector` : '');
            renderLegend();
            render();
        })
        .catch(error => {
            summary.textContent = error.message;
            summary.classList.add('alert');
        });

    search.addEventListener('input', render);

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
        const query = search.value.trim().toLocaleLowerCase();
        points.forEach((point, index) => {
            const visible = activeCategories.has(point.category);
            const match = !query || point.name.toLocaleLowerCase().includes(query);
            const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.setAttribute('cx', 50 + point.x * 900);
            circle.setAttribute('cy', 630 - point.y * 580);
            circle.setAttribute('r', match && query ? '10' : '7');
            circle.setAttribute('fill', colors[categoryIndex(point.category) % colors.length]);
            circle.classList.add('product-map-point');
            if (!visible) circle.classList.add('is-hidden');
            if (query && !match) circle.classList.add('is-dimmed');
            circle.setAttribute('tabindex', visible ? '0' : '-1');
            circle.setAttribute('aria-label', point.name);
            circle.addEventListener('mouseenter', event => showTooltip(event, point));
            circle.addEventListener('mouseleave', hideTooltip);
            circle.addEventListener('focus', event => showTooltip(event, point));
            circle.addEventListener('blur', hideTooltip);
            circle.addEventListener('click', () => showDetails(point));
            svg.append(circle);
        });
    }

    function categoryIndex(category) {
        return [...new Set(points.map(point => point.category))].sort().indexOf(category);
    }

    function showTooltip(event, point) {
        const title = document.createElement('title');
        title.textContent = `${point.name} · ${point.category} · ${formatPrice(point.price)}`;
        event.currentTarget.replaceChildren(title);
    }

    function hideTooltip(event) {
        event.currentTarget.replaceChildren();
    }

    function showDetails(point) {
        details.replaceChildren();
        const heading = document.createElement('h2');
        heading.textContent = point.name;
        const meta = document.createElement('p');
        meta.textContent = `${point.category} · ${formatPrice(point.price)}`;
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
        details.append(heading, meta, title, list);
    }

    function formatPrice(price) {
        return new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(price);
    }
})();
