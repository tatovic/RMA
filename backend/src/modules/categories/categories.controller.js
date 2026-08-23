const categoriesService = require('./categories.service');
const asyncHandler = require('../../utils/asyncHandler');
const { serializeCategory } = require('../../utils/serializer');

const list = asyncHandler(async (req, res) => {
  const categories = await categoriesService.listCategories();
  res.status(200).json({ categories: categories.map(serializeCategory) });
});

const create = asyncHandler(async (req, res) => {
  const category = await categoriesService.createCategory(req.body);
  res.status(201).json(serializeCategory(category));
});

const update = asyncHandler(async (req, res) => {
  const category = await categoriesService.updateCategory(req.params.id, req.body);
  res.status(200).json(serializeCategory(category));
});

const remove = asyncHandler(async (req, res) => {
  await categoriesService.deleteCategory(req.params.id);
  res.status(204).send();
});

module.exports = { list, create, update, remove };
