// Copied from the Bible Planner (core/provider/room/web-worker/worker.js), itself adapted from
// https://github.com/danysantiago/room-web-demo (Apache-2.0), the reference worker for
// androidx.sqlite's WebWorkerSQLiteDriver: runs SQLite WASM in a Web Worker and stores the database in OPFS
// through the opfs-sahpool VFS, which is much faster than the default OPFS VFS and needs no COOP/COEP headers.
// One tab at a time: see installSahPool.
import sqlite3InitModule from '@sqlite.org/sqlite-wasm';

let sqlite3 = null;
let sahPool = null;
let sahPoolError = null;

const SAH_POOL_NAME = 'borarachar-sahpool';
const SAH_POOL_ATTEMPTS = 50;
const SAH_POOL_RETRY_DELAY_MS = 100;

// Maps to track of active database connections and prepared statements by their unique IDs.
const databases = new Map(); // stores databaseId -> SQLiteDbObject
const statements = new Map(); // stores statementId -> SQLiteStatementObject

// Counters to generate unique IDs for new database connections and statements.
let nextDatabaseId = 0;
let nextStatementId = 0;

function openRequest(id, requestData) {
    try {
        if (!sahPool) {
            postMessage({'id': id, error: 'The database is unavailable: ' + sahPoolError});
            return;
        }
        const newDatabaseId = nextDatabaseId++;
        const newDatabase = new sahPool.OpfsSAHPoolDb(toPoolPath(requestData.fileName));
        databases.set(newDatabaseId, newDatabase);
        postMessage({'id': id, data: {'databaseId': newDatabaseId}});
    } catch (error) {
        postMessage({'id': id, error: error.message});
    }
}

function prepareRequest(id, requestData) {
    try {
        const newStatementId = nextStatementId++;
        const resultData = {
            'statementId': newStatementId,
            'parameterCount': 0,
            'columnNames': []
        };
        const database = databases.get(requestData.databaseId);
        if (!database) {
            postMessage({'id': id, error: "Invalid database ID: " + requestData.databaseId});
            return;
        }
        const statement = database.prepare(requestData.sql);
        statements.set(newStatementId, statement);
        resultData.parameterCount = sqlite3.capi.sqlite3_bind_parameter_count(statement);
        for (let i = 0; i < statement.columnCount; i++) {
            resultData.columnNames.push(sqlite3.capi.sqlite3_column_name(statement, i));
        }
        postMessage({'id': id, data: resultData});
    } catch (error) {
        postMessage({'id': id, error: error.message});
    }
}

function stepRequest(id, requestData) {
    const statement = statements.get(requestData.statementId);
    if (!statement) {
        postMessage({'id': id, error: "Invalid statement ID: " + requestData.statementId});
        return;
    }
    try {
        const resultData = {
            'rows': [],
            'columnTypes': []
        };
        statement.reset()
        statement.clearBindings()
        for (let i = 0; i < requestData.bindings.length; i++) {
            statement.bind(i + 1, requestData.bindings[i]);
        }
        while (statement.step()) {
            if (!resultData.columnTypes.length) {
                for (let i = 0; i < statement.columnCount; i++) {
                    resultData.columnTypes.push(sqlite3.capi.sqlite3_column_type(statement, i));
                }
            }
            resultData.rows.push(statement.get([]));
        }
        postMessage({'id': id, data: resultData});
    } catch (error) {
        postMessage({'id': id, error: error.message});
    }
}

function closeRequest(id, requestData) {
    if (requestData.statementId) {
        const statement = statements.get(requestData.statementId);
        if (!statement) {
            postMessage({'id': id, error: "Invalid statement ID: " + requestData.statementId});
            return;
        }
        try {
            statement.finalize();
            statements.delete(requestData.statementId);
        } catch (error) {
            postMessage({'id': id, error: error.message});
        }
    }

    if (requestData.databaseId) {
        const database = databases.get(requestData.databaseId);
        if (!database) {
            postMessage({'id': id, error: "Invalid database ID: " + requestData.databaseId});
            return;
        }
        try {
            database.close();
            databases.delete(requestData.databaseId);
        } catch (error) {
            postMessage({'id': id, error: error.message});
        }
    }
}

// A map that links command names (strings) to their respective handler functions.
const commandMap = {
    'open': openRequest,
    'prepare': prepareRequest,
    'step': stepRequest,
    'close': closeRequest,
};

function handleMessage(e) {
    const requestMsg = e.data;
    if (!Object.hasOwn(requestMsg, 'data') && requestMsg.data == null) {
        postMessage(
            {'id': requestMsg.id, 'error': "Invalid request, missing 'data'."}
        );
        return;
    }
    if (!Object.hasOwn(requestMsg.data, 'cmd') && requestMsg.data.cmd == null) {
        postMessage(
            {'id': requestMsg.id, 'error': "Invalid request, missing 'cmd'."}
        );
        return;
    }
    const command = requestMsg.data.cmd;
    const requestHandler = commandMap[command];
    if (requestHandler) {
        requestHandler(requestMsg.id, requestMsg.data);
    } else {
        postMessage(
            {'id': requestMsg.id, 'error': "Invalid request, unknown command: '" + command + "'."}
        );
    }
}

const messageQueue = [];
onmessage = (e) => {
    if (!sqlite3) {
        messageQueue.push(e);
    } else {
        handleMessage(e);
    }
};

function toPoolPath(fileName) {
    return fileName.startsWith('/') ? fileName : '/' + fileName;
}

// The SAH pool holds exclusive OPFS handles, so only one tab can install it: the page only starts this
// worker once it holds the database lock (bridge/shared.ts). The tab that held it before (a reload, the
// return from a sign in) may release its handles a moment after its lock, hence the retries. There is no
// fallback VFS: the default OPFS one needs cross-origin isolation, which the app doesn't have, and would
// keep a second, separate database anyway.
async function installSahPool(instance) {
    for (let attempt = 1; ; attempt++) {
        try {
            return await instance.installOpfsSAHPoolVfs({
                name: SAH_POOL_NAME,
                // Without it, a failed install deletes the pool, the user's data with it
                preserveOnInitFailure: true,
                forceReinitIfPreviouslyFailed: true,
            });
        } catch (error) {
            if (attempt === SAH_POOL_ATTEMPTS) throw error;
            await new Promise(resolve => setTimeout(resolve, SAH_POOL_RETRY_DELAY_MS));
        }
    }
}

// sqlite3 is only published once the pool is installed (or given up on), so no request is handled before it.
sqlite3InitModule().then(async instance => {
    try {
        sahPool = await installSahPool(instance);
    } catch (error) {
        sahPoolError = error.message;
        console.error('opfs-sahpool unavailable: ' + error.message);
    }
    sqlite3 = instance;
    while (messageQueue.length > 0) {
        handleMessage(messageQueue.shift());
    }
});
