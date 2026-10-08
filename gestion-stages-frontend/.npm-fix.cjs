const Module = require('module');
const orig = Module.prototype.require;
Module.prototype.require = function(id) {
  const res = orig.apply(this, arguments);
  if (id === 'minipass' && typeof res === 'function' && !res.Minipass) {
    res.Minipass = res;
  }
  return res;
};
