/**
 * Explore — photo-first menu and nearby place discovery.
 */
import { api } from '../api.js?v=20261112-2';
import { icon, escapeHtml, skeletonCards } from '../ui.js?v=20261112-2';

const SORTS = [
  { key: 'closest', label: 'Closest' },
  { key: 'price', label: 'Lowest price' },
  { key: 'name', label: 'A–Z' }
];

const CATEGORY_ICONS = {
  food: 'food', groceries: 'bag', health: 'heart', printing: 'printer', banking: 'wallet', grooming: 'salon', fun: 'event', study: 'study'
};
const CATEGORY_ALIASES = {
  'food-drinks': 'food',
  restaurants: 'food',
  beauty: 'grooming',
  salons: 'grooming',
  events: 'fun'
};
const MENU_CATEGORY_LABELS = {
  pizza: 'Pizza',
  burger: 'Burgers',
  chicken: 'Chicken',
  wrap: 'Wraps',
  shawarma: 'Shawarma',
  bakery: 'Bakery',
  snack: 'Snacks',
  drink: 'Drinks'
};

export async function renderExplore(container, params = {}) {
  const filters = {
    q: params.q || '',
    category: params.category
      ? (CATEGORY_ALIASES[params.category] || params.category)
      : 'food',
    menuCategory: '',
    budget: null,
    sort: params.sort === 'name' || params.sort === 'price' ? params.sort : 'closest'
  };

  let places = [];
  let categories = [];

  container.innerHTML = `
    <div class="greet explore-lead">
      <div>
        <p class="greet__hello">Find it in the zone</p>
        <h1 class="greet__title">What are you craving?</h1>
      </div>
    </div>

    <p class="field__hint explore-lead__hint">
      Food photos, published menu prices and places inside 1 km of Richfield.
    </p>

    <form id="explore-search" class="filter-bar">
      <span class="search-pill">
        ${icon('search', 18)}
        <input class="input" type="search" name="q" placeholder="Search pizza, shawarma, scones…"
               aria-label="Search menu items and places" value="${escapeHtml(filters.q)}">
      </span>
    </form>

    <div class="chips" id="category-chips" aria-label="Place categories"></div>
    <div class="chips menu-filters" id="menu-category-chips" aria-label="Food types"></div>
    <div class="chips budget-filters" id="budget-chips" aria-label="Maximum menu price">
      <button class="chip is-active" type="button" data-budget="">Any price</button>
      <button class="chip" type="button" data-budget="30">Up to R30</button>
      <button class="chip" type="button" data-budget="50">Up to R50</button>
      <button class="chip" type="button" data-budget="100">Up to R100</button>
    </div>
    <div class="chips" id="sort-chips" aria-label="Sort results">
      ${SORTS.map((sort) => `
        <button class="chip ${sort.key === filters.sort ? 'is-active' : ''}" type="button"
                data-sort="${sort.key}">${sort.label}</button>`).join('')}
    </div>

    <div class="row row--between explore-count-row">
      <span class="small muted" id="result-count">Loading nearby menus…</span>
      <button class="btn--link small" type="button" id="clear-filters" hidden>Clear</button>
    </div>

    <div id="results">${skeletonCards(4)}</div>`;

  const input = container.querySelector('#explore-search input');
  const categoryChips = container.querySelector('#category-chips');
  const menuCategoryChips = container.querySelector('#menu-category-chips');
  const budgetChips = container.querySelector('#budget-chips');
  const results = container.querySelector('#results');
  const count = container.querySelector('#result-count');
  const clear = container.querySelector('#clear-filters');

  try {
    const [placeResult, configResult] = await Promise.all([
      api.assistantPlaces(),
      api.assistantConfig()
    ]);
    places = (placeResult.data || []).filter((place) => place.withinRadius === true);
    categories = (configResult.data?.categories || [])
      .filter((category) => places.some((place) => place.category === category.id));
    if (!categories.some((category) => category.id === filters.category)) filters.category = 'food';

    categoryChips.innerHTML = `
      <button class="chip ${filters.category ? '' : 'is-active'}" type="button" data-category="">All places</button>
      ${categories.map((category) => `
        <button class="chip ${filters.category === category.id ? 'is-active' : ''}" type="button"
                data-category="${escapeHtml(category.id)}">${escapeHtml(category.label)}</button>`).join('')}`;

    const menuCategories = [...new Set(places.filter((place) => place.category === 'food')
      .flatMap((place) => (place.menuItems || []).map((item) => item.category)))];
    menuCategoryChips.innerHTML = `
      <button class="chip is-active" type="button" data-menu-category="">All food</button>
      ${menuCategories.map((category) => `
        <button class="chip" type="button" data-menu-category="${escapeHtml(category)}">
          ${escapeHtml(MENU_CATEGORY_LABELS[category] || category[0].toUpperCase() + category.slice(1))}
        </button>`).join('')}`;

    categoryChips.addEventListener('click', (event) => {
      const chip = event.target.closest('[data-category]');
      if (!chip) return;
      filters.category = chip.dataset.category;
      categoryChips.querySelectorAll('[data-category]')
        .forEach((node) => node.classList.toggle('is-active', node === chip));
      updateMenuFilters();
      renderResults();
    });

    menuCategoryChips.addEventListener('click', (event) => {
      const chip = event.target.closest('[data-menu-category]');
      if (!chip) return;
      filters.menuCategory = chip.dataset.menuCategory;
      menuCategoryChips.querySelectorAll('[data-menu-category]')
        .forEach((node) => node.classList.toggle('is-active', node === chip));
      renderResults();
    });

    budgetChips.addEventListener('click', (event) => {
      const chip = event.target.closest('[data-budget]');
      if (!chip) return;
      filters.budget = chip.dataset.budget ? Number(chip.dataset.budget) : null;
      budgetChips.querySelectorAll('[data-budget]')
        .forEach((node) => node.classList.toggle('is-active', node === chip));
      renderResults();
    });

    container.querySelector('#sort-chips').addEventListener('click', (event) => {
      const chip = event.target.closest('[data-sort]');
      if (!chip) return;
      filters.sort = chip.dataset.sort;
      container.querySelectorAll('[data-sort]')
        .forEach((node) => node.classList.toggle('is-active', node === chip));
      renderResults();
    });

    input.addEventListener('input', () => {
      filters.q = input.value.trim();
      clearTimeout(renderResults.timer);
      renderResults.timer = setTimeout(renderResults, 160);
    });

    container.querySelector('#explore-search').addEventListener('submit', (event) => {
      event.preventDefault();
      filters.q = input.value.trim();
      renderResults();
    });

    clear.addEventListener('click', () => {
      filters.q = '';
      filters.category = 'food';
      filters.menuCategory = '';
      filters.budget = null;
      input.value = '';
      categoryChips.querySelectorAll('[data-category]')
        .forEach((node) => node.classList.toggle('is-active', node.dataset.category === 'food'));
      budgetChips.querySelectorAll('[data-budget]')
        .forEach((node) => node.classList.toggle('is-active', node.dataset.budget === ''));
      updateMenuFilters();
      renderResults();
    });

    updateMenuFilters();
    renderResults();
  } catch (error) {
    count.textContent = 'Nearby places could not be loaded';
    results.innerHTML = `<div class="card empty">
      <span class="empty__icon">${icon('alert', 22)}</span>
      <strong>Could not load the nearby directory</strong>
      <p class="muted small">${escapeHtml(error.message)}</p>
    </div>`;
  }

  function updateMenuFilters() {
    const showMenuFilters = !filters.category || filters.category === 'food';
    menuCategoryChips.hidden = !showMenuFilters;
    budgetChips.hidden = !showMenuFilters;
    if (!showMenuFilters) {
      filters.menuCategory = '';
      filters.budget = null;
    }
    menuCategoryChips.querySelectorAll('[data-menu-category]')
      .forEach((node) => node.classList.toggle('is-active', node.dataset.menuCategory === filters.menuCategory));
    budgetChips.querySelectorAll('[data-budget]')
      .forEach((node) => node.classList.toggle('is-active', node.dataset.budget === (filters.budget === null ? '' : String(filters.budget))));
  }

  function renderResults() {
    const needle = normalise(filters.q);
    const foodPlaces = places.filter((place) => place.category === 'food');
    const allMenuItems = foodPlaces.flatMap((place) => (place.menuItems || [])
      .map((item) => ({ ...item, placeName: place.name, placeId: place.id,
        distanceMeters: place.distanceMeters, placeImage: place.image, approximateDistance: /approx|pin supplied|exact street address not provided/i.test(place.verificationNote || '') })));
    const menuMode = !filters.category || filters.category === 'food';
    const matchedItems = menuMode ? allMenuItems.filter((item) => {
      const hasPrice = item.price !== null && item.price !== undefined && Number.isFinite(Number(item.price));
      const matchesBudget = filters.budget === null || (hasPrice && Number(item.price) <= filters.budget);
      const matchesMenuType = !filters.menuCategory || item.category === filters.menuCategory;
      const searchable = normalise([
        item.name, item.category, item.description, item.placeName
      ].filter(Boolean).join(' '));
      return matchesBudget && matchesMenuType && (!needle || searchable.includes(needle));
    }).sort((a, b) => {
      if (filters.sort === 'name') return a.name.localeCompare(b.name);
      if (filters.sort === 'price') {
        const aPrice = a.price === null || a.price === undefined ? Infinity : Number(a.price);
        const bPrice = b.price === null || b.price === undefined ? Infinity : Number(b.price);
        return aPrice - bPrice || a.distanceMeters - b.distanceMeters;
      }
      return a.distanceMeters - b.distanceMeters
        || Number(a.price ?? Infinity) - Number(b.price ?? Infinity);
    }) : [];

    const matchedPlaces = places.filter((place) => {
      if (menuMode) return false;
      const matchesCategory = !filters.category || place.category === filters.category;
      const searchable = normalise([
        place.name, place.address, place.categoryLabel, place.description
      ].filter(Boolean).join(' '));
      return matchesCategory && (!needle || searchable.includes(needle));
    }).sort((a, b) => filters.sort === 'name'
      ? a.name.localeCompare(b.name)
      : (a.distanceMeters || 0) - (b.distanceMeters || 0));

    const active = Boolean(filters.q || filters.budget !== null || filters.menuCategory);
    clear.hidden = !active;
    if (menuMode) {
      count.textContent = `${matchedItems.length} menu item${matchedItems.length === 1 ? '' : 's'} · ${foodPlaces.length} nearby food places`;
      const noMenuPlaces = foodPlaces.filter((place) => !(place.menuItems || []).length
        && (!needle || normalise(`${place.name} ${place.address}`).includes(needle)));
      const otherPlaces = !filters.category
        ? places.filter((place) => place.category !== 'food')
        : [];
      const itemMarkup = matchedItems.length
        ? `<div class="menu-grid">${matchedItems.map(menuTile).join('')}</div>`
        : `<div class="card empty menu-empty">
             <span class="empty__icon">${icon('search', 22)}</span>
             <strong>No menu items match</strong>
             <p class="muted small">${filters.budget !== null
               ? `Try a higher budget, or check the full menu at a nearby place.`
               : 'Try another food type or search.'}</p>
           </div>`;
      const missingMarkup = noMenuPlaces.length ? `
        <section class="explore-subsection">
          <div class="section-head"><h2>More food spots</h2><span class="muted small">Menu not published yet</span></div>
          <div class="venue-grid">${noMenuPlaces.map(placeCard).join('')}</div>
        </section>` : '';
      const otherMarkup = otherPlaces.length ? `
        <section class="explore-subsection">
          <div class="section-head"><h2>More nearby</h2><span class="muted small">${otherPlaces.length} places</span></div>
          <div class="venue-grid">${otherPlaces.map(placeCard).join('')}</div>
        </section>` : '';
      results.innerHTML = itemMarkup + missingMarkup + otherMarkup;
    } else {
      count.textContent = `${matchedPlaces.length} nearby place${matchedPlaces.length === 1 ? '' : 's'}`;
      results.innerHTML = matchedPlaces.length
        ? `<div class="venue-grid">${matchedPlaces.map(placeCard).join('')}</div>`
        : `<div class="card empty">
             <span class="empty__icon">${icon('search', 22)}</span>
             <strong>No nearby place matches</strong>
             <p class="muted small">Try another search or category.</p>
           </div>`;
    }
    results.querySelectorAll('img').forEach((img) => {
      img.addEventListener('error', () => img.classList.add('is-broken'), { once: true });
    });
  }
}

export function placeCard(place) {
  const glyph = CATEGORY_ICONS[place.category] || 'pin';
  const media = place.image
    ? `<img src="${escapeHtml(place.image)}" alt="${escapeHtml(place.name)}" loading="lazy">
       <span class="venue-tile__fallback">${icon(glyph, 26)}</span>`
    : `<span class="venue-tile__fallback">${icon(glyph, 26)}</span>`;
  const distance = place.distanceMeters
    ? `${place.verificationNote && /approx|pin supplied|exact street address not provided/i.test(place.verificationNote) ? '≈ ' : ''}${place.distanceMeters} m`
    : 'On campus';
  const noMenu = place.category === 'food' && !(place.menuItems || []).length;

  return `
    <a class="venue-tile" href="#/place/${encodeURIComponent(place.id)}">
      <div class="venue-tile__media">${media}</div>
      <div class="venue-tile__body">
        <strong>${escapeHtml(place.name)}</strong>
        <span>${escapeHtml(distance)} away · ${escapeHtml(place.categoryLabel || 'Place')}</span>
        ${noMenu ? '<span class="venue-tile__note">Menu not published yet</span>' : ''}
      </div>
    </a>`;
}

function menuTile(item) {
  const price = item.price !== null && item.price !== undefined && Number.isFinite(Number(item.price))
    ? `R${Number(item.price).toFixed(2)}`
    : 'Check menu';
  const imageUrl = item.image || item.placeImage;
  const imageAlt = item.image ? item.name : `${item.placeName} storefront`;
  const image = imageUrl
    ? `<img class="menu-tile__photo" src="${escapeHtml(imageUrl)}"
            alt="${escapeHtml(imageAlt)}" loading="lazy">`
    : '';
  const glyph = 'food';
  const distance = Number(item.distanceMeters || 0) + ' m';
  const distanceLabel = item.approximateDistance ? '≈ ' + distance : distance;
  return `
    <a class="menu-tile" href="#/place/${encodeURIComponent(item.placeId)}"
       aria-label="${escapeHtml(item.name)}, ${escapeHtml(price)}, at ${escapeHtml(item.placeName)}">
      <div class="menu-tile__media">
        <span class="menu-tile__fallback">${icon(glyph, 25)}</span>
        ${image}
        <span class="menu-tile__price">${escapeHtml(price)}</span>
      </div>
      <div class="menu-tile__body">
        <strong>${escapeHtml(item.name)}</strong>
        <span>${escapeHtml(item.placeName)} · ${escapeHtml(distanceLabel)}</span>
      </div>
    </a>`;
}

function normalise(value) {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}
