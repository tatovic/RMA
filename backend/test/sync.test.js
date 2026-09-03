const { test, after, describe } = require('node:test');
const assert = require('node:assert/strict');

const {
  api,
  pool,
  uuid,
  authHeader,
  itemPayload,
  createItem,
  seededUser,
  closePool,
} = require('./helpers');
const { DELTA_PAGE_SIZE } = require('../src/modules/items/items.service');

after(closePool);

// Regresije za tiket 28 — svaki test ovde pokriva jedan konkretan nalaz iz revizije komita 1077837.

describe('Delta pull straničenje (nalaz 01)', () => {
  // Redovi se upisuju direktno u bazu: 600 POST zahteva bi test pretvorilo u minut čekanja, a ono
  // što se proverava je isključivo ponašanje čitanja.
  const insertItems = async (user, count, baseTime, { sameMillisecondTail = 0 } = {}) => {
    const rows = [];
    for (let i = 0; i < count; i += 1) {
      const offsetMs = i < count - sameMillisecondTail ? i : count - sameMillisecondTail - 1;
      const updatedAt = new Date(baseTime.getTime() + offsetMs);
      rows.push([
        uuid(),
        user.user.id,
        `Predmet ${i}`,
        user.categoryId,
        user.locationId,
        1,
        'RSD',
        updatedAt,
        updatedAt,
      ]);
    }
    await pool.query(
      `INSERT INTO inventory_items
         (id, user_id, name, category_id, location_id, quantity, currency, created_at, updated_at)
       VALUES ?`,
      [rows]
    );
    return rows.map((row) => row[0]);
  };

  test('strana preko limita vraća hasMore i nextSince koji ne preskače nijedan red', async () => {
    const user = await seededUser('paging');
    const total = DELTA_PAGE_SIZE + 100;
    const base = new Date(Date.now() + 1000); // u budućnosti, da `since` ispod sigurno bude stariji
    const insertedIds = await insertItems(user, total, base);

    const seen = new Set();
    let since = new Date(base.getTime() - 1).toISOString();
    let pages = 0;
    let hasMore = true;

    while (hasMore) {
      const response = await api()
        .get('/api/items')
        .query({ since })
        .set(authHeader(user.token))
        .expect(200);

      assert.ok(response.body.nextSince, 'odgovor mora nositi nextSince');
      assert.equal(typeof response.body.hasMore, 'boolean', 'odgovor mora nositi hasMore');

      response.body.items.forEach((item) => seen.add(item.id));
      since = response.body.nextSince;
      hasMore = response.body.hasMore;
      pages += 1;
      assert.ok(pages < 10, 'straničenje ne sme da se vrti u krug');
    }

    assert.ok(pages >= 2, `${total} predmeta mora stati u više od jedne strane, dobijeno ${pages}`);
    insertedIds.forEach((id) => {
      assert.ok(seen.has(id), `predmet ${id} je preskočen tokom straničenja`);
    });
  });

  test('prva strana ne preseca grupu redova sa istim updated_at', async () => {
    const user = await seededUser('paging-tie');
    const tail = 5;
    const total = DELTA_PAGE_SIZE + 50;
    const base = new Date(Date.now() + 1000);
    await insertItems(user, total, base, { sameMillisecondTail: tail });

    const since = new Date(base.getTime() - 1).toISOString();
    const response = await api().get('/api/items').query({ since }).set(authHeader(user.token)).expect(200);

    assert.equal(response.body.hasMore, true);

    // Poslednji updated_at na strani mora biti STROGO stariji od vremena koje deli rep grupe —
    // inače bi sledeći `since` (updated_at > ?) preskočio braću iz iste milisekunde.
    const lastOnPage = response.body.items[response.body.items.length - 1].updatedAt;
    const groupTime = new Date(base.getTime() + total - tail - 1).toISOString();
    assert.notEqual(lastOnPage, groupTime, 'strana se završila usred grupe sa istim updated_at');
    assert.equal(response.body.nextSince, lastOnPage);
  });

  test('iscrpljena delta vraća hasMore=false i nextSince jednak serverTime', async () => {
    const user = await seededUser('paging-drained');
    await createItem(
      user.token,
      itemPayload({ categoryId: user.categoryId, locationId: user.locationId })
    );

    const response = await api()
      .get('/api/items')
      .query({ since: new Date(Date.now() + 60000).toISOString() })
      .set(authHeader(user.token))
      .expect(200);

    assert.equal(response.body.items.length, 0);
    assert.equal(response.body.hasMore, false);
    assert.equal(response.body.nextSince, response.body.serverTime);
  });

  test('pull bez since vraća živ inventar bez tombstone redova', async () => {
    const user = await seededUser('full-pull');
    const kept = await createItem(
      user.token,
      itemPayload({ categoryId: user.categoryId, locationId: user.locationId, name: 'Ostaje' })
    );
    const removed = await createItem(
      user.token,
      itemPayload({ categoryId: user.categoryId, locationId: user.locationId, name: 'Briše se' })
    );
    await api().delete(`/api/items/${removed.id}`).set(authHeader(user.token)).expect(204);

    const response = await api().get('/api/items').set(authHeader(user.token)).expect(200);
    const ids = response.body.items.map((item) => item.id);

    assert.ok(ids.includes(kept.id));
    assert.equal(ids.includes(removed.id), false, 'pun pull ne sme da vraća soft-obrisane predmete');
    assert.equal(response.body.hasMore, false);
    assert.equal(response.body.nextSince, response.body.serverTime);
  });
});

describe('Brisanje lokacije i kategorije uz tombstone redove (nalaz 02)', () => {
  test('lokacija čiji su svi predmeti soft-obrisani se briše', async () => {
    const user = await seededUser('loc-tombstone');
    const locationResponse = await api()
      .post('/api/locations')
      .set(authHeader(user.token))
      .send({ id: uuid(), name: `Garaža ${uuid().slice(0, 8)}` })
      .expect(201);
    const locationId = locationResponse.body.id;

    const item = await createItem(user.token, itemPayload({ categoryId: user.categoryId, locationId }));
    await api().delete(`/api/items/${item.id}`).set(authHeader(user.token)).expect(204);

    await api().delete(`/api/locations/${locationId}`).set(authHeader(user.token)).expect(204);

    const [rows] = await pool.query('SELECT id FROM inventory_items WHERE id = ?', [item.id]);
    assert.equal(rows.length, 0, 'kaskada mora ukloniti i tombstone red predmeta');
  });

  test('lokacija sa živim predmetom se i dalje ne briše (BR-014)', async () => {
    const user = await seededUser('loc-in-use');
    const locationResponse = await api()
      .post('/api/locations')
      .set(authHeader(user.token))
      .send({ id: uuid(), name: `Podrum ${uuid().slice(0, 8)}` })
      .expect(201);
    const locationId = locationResponse.body.id;

    const item = await createItem(user.token, itemPayload({ categoryId: user.categoryId, locationId }));

    const response = await api()
      .delete(`/api/locations/${locationId}`)
      .set(authHeader(user.token))
      .expect(409);

    assert.equal(response.body.error.code, 'LOCATION_IN_USE');
    assert.equal(response.body.error.details.itemCount, 1);

    const [rows] = await pool.query('SELECT id FROM inventory_items WHERE id = ?', [item.id]);
    assert.equal(rows.length, 1, 'odbijeno brisanje ne sme ništa da ukloni');
  });
});

describe('Rešavanje konflikta (nalaz 03, DB-RULE-04)', () => {
  test('zastareo updatedAt se odbija sa 409 i vraća serversku verziju', async () => {
    const user = await seededUser('conflict');
    const created = await createItem(
      user.token,
      itemPayload({ categoryId: user.categoryId, locationId: user.locationId, name: 'Prva verzija' })
    );

    const staleUpdatedAt = created.updatedAt;

    const updated = await api()
      .put(`/api/items/${created.id}`)
      .set(authHeader(user.token))
      .send({
        ...itemPayload({
          id: created.id,
          categoryId: user.categoryId,
          locationId: user.locationId,
          name: 'Druga verzija',
        }),
        updatedAt: created.updatedAt,
      })
      .expect(200);

    assert.notEqual(updated.body.updatedAt, staleUpdatedAt, 'server mora izdati novi updatedAt');

    const conflict = await api()
      .put(`/api/items/${created.id}`)
      .set(authHeader(user.token))
      .send({
        ...itemPayload({
          id: created.id,
          categoryId: user.categoryId,
          locationId: user.locationId,
          name: 'Zakasnela verzija',
        }),
        updatedAt: staleUpdatedAt,
      })
      .expect(409);

    assert.equal(conflict.body.error.code, 'SYNC_CONFLICT');
    assert.equal(conflict.body.error.details.id, created.id);
    assert.equal(conflict.body.error.details.name, 'Druga verzija', 'vraća se serverska verzija reda');
  });

  test('updatedAt daleko u budućnosti se odbija umesto da dobije svaki konflikt', async () => {
    const user = await seededUser('skew');
    const created = await createItem(
      user.token,
      itemPayload({ categoryId: user.categoryId, locationId: user.locationId })
    );

    const response = await api()
      .put(`/api/items/${created.id}`)
      .set(authHeader(user.token))
      .send({
        ...itemPayload({ id: created.id, categoryId: user.categoryId, locationId: user.locationId }),
        updatedAt: new Date(Date.now() + 72 * 60 * 60 * 1000).toISOString(),
      })
      .expect(400);

    assert.equal(response.body.error.code, 'VALIDATION_ERROR');
  });
});
