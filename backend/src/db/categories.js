// Jedanaest globalnih kategorija — db.md sekcija 4.1. Redosled je obavezujući (sort_order).
const CATEGORIES = [
  { sortOrder: 1, name: 'Elektronika', iconKey: 'ic_category_electronics' },
  { sortOrder: 2, name: 'Nameštaj', iconKey: 'ic_category_furniture' },
  { sortOrder: 3, name: 'Bela tehnika', iconKey: 'ic_category_appliances' },
  { sortOrder: 4, name: 'Kuhinja', iconKey: 'ic_category_kitchen' },
  { sortOrder: 5, name: 'Odeća', iconKey: 'ic_category_clothing' },
  { sortOrder: 6, name: 'Alat', iconKey: 'ic_category_tools' },
  { sortOrder: 7, name: 'Sportska oprema', iconKey: 'ic_category_sports' },
  { sortOrder: 8, name: 'Vozila', iconKey: 'ic_category_vehicles' },
  { sortOrder: 9, name: 'Dekoracija', iconKey: 'ic_category_decor' },
  { sortOrder: 10, name: 'Dokumenta', iconKey: 'ic_category_documents' },
  { sortOrder: 11, name: 'Ostalo', iconKey: 'ic_category_other' },
];

module.exports = CATEGORIES;
