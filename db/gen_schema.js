// 从 roncoo-education 的 MyBatis Mapper XML 反向重建 MySQL 建表 SQL
const fs = require('fs');
const path = require('path');

const ROOT = 'D:/视频培训/roncoo-education';
const OUT_DIR = path.join(ROOT, 'db');
const OUT = path.join(OUT_DIR, 'schema_rebuild.sql');

function walk(dir, acc) {
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name);
    let st;
    try { st = fs.statSync(p); } catch { continue; }
    if (st.isDirectory()) {
      if (['target', '.git', 'node_modules'].includes(name)) continue;
      walk(p, acc);
    } else if (/Mapper\.xml$/.test(name)) {
      acc.push(p);
    }
  }
  return acc;
}

function attr(tag, name) {
  const m = tag.match(new RegExp(name + '\\s*=\\s*"([^"]*)"'));
  return m ? m[1] : null;
}

function jdbcToMysql(jdbc, col) {
  const t = (jdbc || '').toUpperCase();
  const c = (col || '').toLowerCase();
  const longNames = /(content|detail|describe|description|intro|introduce|remark|json|ext|params|param|body|html|address)/;
  switch (t) {
    case 'BIGINT': return 'bigint';
    case 'INTEGER': return 'int';
    case 'TINYINT': return 'tinyint';
    case 'SMALLINT': return 'smallint';
    case 'DECIMAL':
    case 'NUMERIC': return 'decimal(12,2)';
    case 'DOUBLE':
    case 'REAL': return 'double';
    case 'FLOAT': return 'float';
    case 'BIT':
    case 'BOOLEAN': return 'tinyint(1)';
    case 'DATE': return 'date';
    case 'TIME': return 'time';
    case 'TIMESTAMP': return 'datetime';
    case 'LONGVARCHAR':
    case 'LONGNVARCHAR':
    case 'CLOB': return 'text';
    case 'BLOB':
    case 'LONGVARBINARY': return 'longblob';
    case 'CHAR':
    case 'NCHAR':
    case 'VARCHAR':
    case 'NVARCHAR':
    default:
      return longNames.test(c) ? 'text' : 'varchar(255)';
  }
}

const files = walk(ROOT, []);
const tables = [];
for (const f of files) {
  const xml = fs.readFileSync(f, 'utf8');
  let table = null;
  let m = xml.match(/insert\s+into\s+([a-zA-Z_][a-zA-Z0-9_]*)/i);
  if (m) table = m[1];
  if (!table) { m = xml.match(/\bfrom\s+([a-zA-Z_][a-zA-Z0-9_]*)/i); if (m) table = m[1]; }
  if (!table) continue;

  const cols = new Map(); // col -> {jdbc, isId}
  const rmRe = /<resultMap[\s\S]*?<\/resultMap>/g;
  let rm;
  while ((rm = rmRe.exec(xml)) !== null) {
    const block = rm[0];
    const tagRe = /<(id|result)\s+[^>]*?\/?>/g;
    let tag;
    while ((tag = tagRe.exec(block)) !== null) {
      const isId = tag[0].startsWith('<id');
      const col = attr(tag[0], 'column');
      const jdbc = attr(tag[0], 'jdbcType');
      if (!col) continue;
      if (!cols.has(col)) cols.set(col, { jdbc, isId });
      else if (isId) cols.get(col).isId = true;
    }
  }
  if (cols.size === 0) continue;
  tables.push({ table, cols });
}

tables.sort((a, b) => a.table.localeCompare(b.table));

let sql = '';
sql += '-- roncoo-education 数据库结构（从 MyBatis Mapper XML 的 resultMap 反向重建）\n';
sql += '-- 生成时间: ' + new Date().toISOString() + '\n';
sql += '-- 说明: 字段名/类型来自 resultMap；主键由应用生成(无 AUTO_INCREMENT)；\n';
sql += '--       字段长度/索引/默认值/初始数据为重建推断，非 roncoo 原版，需边跑边校对。\n';
sql += 'SET NAMES utf8mb4;\n';
sql += 'SET FOREIGN_KEY_CHECKS=0;\n\n';

for (const { table, cols } of tables) {
  sql += `-- ${table} (${cols.size} 字段)\n`;
  sql += `DROP TABLE IF EXISTS \`${table}\`;\n`;
  sql += `CREATE TABLE \`${table}\` (\n`;
  const lines = [];
  const pks = [];
  for (const [col, info] of cols) {
    const type = jdbcToMysql(info.jdbc, col);
    if (info.isId) {
      lines.push(`  \`${col}\` ${type} NOT NULL`);
      pks.push(col);
    } else if (col === 'gmt_create') {
      lines.push(`  \`${col}\` datetime DEFAULT CURRENT_TIMESTAMP`);
    } else if (col === 'gmt_modified') {
      lines.push(`  \`${col}\` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`);
    } else {
      lines.push(`  \`${col}\` ${type} DEFAULT NULL`);
    }
  }
  if (pks.length) lines.push(`  PRIMARY KEY (${pks.map(p => '`' + p + '`').join(', ')})`);
  sql += lines.join(',\n') + '\n';
  sql += `) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='${table}';\n\n`;
}
sql += 'SET FOREIGN_KEY_CHECKS=1;\n';

fs.mkdirSync(OUT_DIR, { recursive: true });
fs.writeFileSync(OUT, sql, 'utf8');
console.log('解析 Mapper 文件:', files.length);
console.log('生成表数:', tables.length);
console.log('输出:', OUT);
console.log('---');
for (const t of tables) console.log('  ' + t.table.padEnd(26) + t.cols.size + ' 字段');
