const itemsService = require('./items.service');
const asyncHandler = require('../../utils/asyncHandler');
const { serializeItem, toISOString } = require('../../utils/serializer');

// `nextSince` je vrednost koju klijent pamti za sledeći delta pull, `hasMore` da li ga odmah ponavlja
// (tiket 28, nalaz 01). `serverTime` ostaje u odgovoru zbog kompatibilnosti sa starijim klijentom.
const list = asyncHandler(async (req, res) => {
  const { items, serverTime, nextSince, hasMore } = await itemsService.listItems(req.userId, req.query.since);
  res.status(200).json({
    items: items.map(serializeItem),
    serverTime: toISOString(serverTime),
    nextSince: toISOString(nextSince),
    hasMore,
  });
});

const create = asyncHandler(async (req, res) => {
  const { item, created } = await itemsService.createItem(req.userId, req.body);
  res.status(created ? 201 : 200).json(serializeItem(item));
});

const getOne = asyncHandler(async (req, res) => {
  const item = await itemsService.getItem(req.userId, req.params.id);
  res.status(200).json(serializeItem(item));
});

const update = asyncHandler(async (req, res) => {
  const item = await itemsService.updateItem(req.userId, req.params.id, req.body);
  res.status(200).json(serializeItem(item));
});

const remove = asyncHandler(async (req, res) => {
  await itemsService.deleteItem(req.userId, req.params.id);
  res.status(204).send();
});

module.exports = { list, create, getOne, update, remove };
