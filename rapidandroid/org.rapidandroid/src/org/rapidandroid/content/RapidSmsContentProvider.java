package org.rapidandroid.content;

import org.rapidandroid.content.translation.MessageTranslator;
import org.rapidandroid.content.translation.ModelTranslator;
import org.rapidandroid.data.RapidSmsDataDefs;
import org.rapidandroid.data.SmsDbHelper;
import org.rapidsms.java.core.model.Form;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;

import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * 
 *  
 * @author Daniel Myung dmyung@dimagi.com
 * @created Jan 09, 2009
 * 
 * Main Content provider for the RapidAndroid project.
 * 
 * It should be the universal URI accessible content provider that's accessible via the GetContentResolver().query()
 * 
 * The definitions for the URI are linked via the RapidSmsDataDefs static properties.
 * 
 * This content provider should provide all functionality to do the following:
 *  - insert/query SMS messages that are known to be related to this app
 *  - insert/query monitors (senders) in a table associated with the SMSs
 *  - insert/query Form/Field/Fieldtype definitions in SQL that reflect the structures defined in the org.rapidsms.java.core.model.*
 *  - Create and store and query data tables generated by the Form definition.
 *   
 */

public class RapidSmsContentProvider extends ContentProvider {
	/**
	 * @param context
	 * @param name
	 * @param factory
	 * @param version
	 */

	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ RapidSmsDataDefs.AUTHORITY);

	private static final String TAG = "RapidSmsContentProvider";

	private SmsDbHelper mOpenHelper;

	private static final int MESSAGE = 1;
	private static final int MESSAGE_ID = 2;
	private static final int MONITOR = 3;
	private static final int MONITOR_ID = 4;
	private static final int MONITOR_MESSAGE_ID = 5;

	private static final int FORM = 6;
	private static final int FORM_ID = 7;

	private static final int FIELD = 8;
	private static final int FIELD_ID = 9;

	private static final int FIELDTYPE = 10;
	private static final int FIELDTYPE_ID = 11;

	private static final int FORMDATA_ID = 12;
	// private static final int FORMDATA_ID = 13;

	private static final UriMatcher sUriMatcher;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.Message.URI_PART, MESSAGE);
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.Message.URI_PART + "/#", MESSAGE_ID);

		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.Monitor.URI_PART, MONITOR);
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.Monitor.URI_PART + "/#", MONITOR_ID);
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY, "messagesbymonitor/#",
				MONITOR_MESSAGE_ID);

		// form field data stuffs
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.Form.URI_PART, FORM);
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.Form.URI_PART + "/#", FORM_ID);

		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.Field.URI_PART, FIELD);
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.Field.URI_PART + "/#", FIELD_ID);

		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.FieldType.URI_PART, FIELDTYPE);
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.FieldType.URI_PART + "/#", FIELDTYPE_ID);

		// actual form data
		sUriMatcher.addURI(RapidSmsDataDefs.AUTHORITY,
				RapidSmsDataDefs.FormData.URI_PART + "/#", FORMDATA_ID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case MESSAGE:
			return RapidSmsDataDefs.Message.CONTENT_TYPE;
		case MESSAGE_ID:
			return RapidSmsDataDefs.Message.CONTENT_ITEM_TYPE;
		case MONITOR:
			return RapidSmsDataDefs.Monitor.CONTENT_TYPE;
		case MONITOR_ID:
			return RapidSmsDataDefs.Monitor.CONTENT_ITEM_TYPE;
		case MONITOR_MESSAGE_ID:
			// this is similar to Monitor, but is filtered
			return RapidSmsDataDefs.Monitor.CONTENT_TYPE;

		case FORM:
			return RapidSmsDataDefs.Form.CONTENT_TYPE;
		case FORM_ID:
			return RapidSmsDataDefs.Form.CONTENT_ITEM_TYPE;

		case FIELD:
			return RapidSmsDataDefs.Field.CONTENT_TYPE;
		case FIELD_ID:
			return RapidSmsDataDefs.Field.CONTENT_ITEM_TYPE;

		case FIELDTYPE:
			return RapidSmsDataDefs.FieldType.CONTENT_TYPE;
		case FIELDTYPE_ID:
			return RapidSmsDataDefs.FieldType.CONTENT_ITEM_TYPE;

		case FORMDATA_ID:
			return RapidSmsDataDefs.FormData.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
			// return sUriMatcher.match(uri)+"";
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// Validate the requested uri
		// if (sUriMatcher.match(uri) != MESSAGE || sUriMatcher.match(uri) !=
		// MONITOR) {
		// throw new IllegalArgumentException("Unknown URI " + uri);
		// }

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		switch (sUriMatcher.match(uri)) {
		case MESSAGE:
			return insertMessage(uri, values);
		case MONITOR:
			return insertMonitor(uri, values);
		case FIELDTYPE:
			return insertFieldType(uri, values);
		case FIELD:
			return insertField(uri, values);
		case FORM:
			return insertForm(uri, values);
		case FORMDATA_ID:
			return insertFormData(uri, values);
			// other stuffs not implemented for insertion yet.

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);

		}
	}

	private Uri insertFormData(Uri uri, ContentValues values) {
		// sanity check, see if the table exists
		String formid = uri.getPathSegments().get(1);
		String formprefix = ModelTranslator.getFormById(Integer.valueOf(formid).intValue()).getPrefix();
		SQLiteDatabase dbr = mOpenHelper.getReadableDatabase();
		Cursor table_exists = dbr.rawQuery("select count(*) from formdata_"
				+ formprefix, null);
		if (table_exists.getCount() != 1) {
			throw new SQLException("Failed to insert row into " + uri
					+ " :: table doesn't exist.");
		}
		table_exists.close();

		//doInsert doesn't apply well here.		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(RapidSmsDataDefs.FormData.TABLE_PREFIX + formprefix,
				RapidSmsDataDefs.FormData.MESSAGE, values);
		if (rowId > 0) {
			Uri fieldUri = ContentUris.withAppendedId(
					RapidSmsDataDefs.Form.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(fieldUri, null);
			return Uri.parse(uri.toString() + "/" + rowId);
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}
	
	private Uri insertForm(Uri uri, ContentValues values) {
		if (values.containsKey(RapidSmsDataDefs.Form.FORMNAME) == false
				|| values.containsKey(RapidSmsDataDefs.Form.DESCRIPTION) == false
				|| values.containsKey(RapidSmsDataDefs.Form.PARSEMETHOD) == false) {
			throw new SQLException("Insufficient arguments for Form insert "
					+ uri);
		}		
		return doInsert(uri, values, RapidSmsDataDefs.Form.TABLE,RapidSmsDataDefs.Form.FORMNAME);
	}

	/**
	 * @param uri
	 * @param values
	 * @return
	 */
	private Uri doInsert(Uri uri, ContentValues values, String tablename, String nullvalue) {
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();	
		
		long rowId = db.insert(tablename,
				nullvalue, values);
		if (rowId > 0) {
			Uri retUri = ContentUris.withAppendedId(
					uri, rowId);
			getContext().getContentResolver().notifyChange(retUri, null);
			return retUri;
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}
	
	//Insert Methods
	private Uri insertField(Uri uri, ContentValues values) {
		if (values.containsKey(RapidSmsDataDefs.Field.FORM) == false
				|| values.containsKey(RapidSmsDataDefs.Field.NAME) == false
				|| values.containsKey(RapidSmsDataDefs.Field.FIELDTYPE) == false
				|| values.containsKey(RapidSmsDataDefs.Field.PROMPT) == false
				|| values.containsKey(RapidSmsDataDefs.Field.SEQUENCE) == false) {
			throw new SQLException("Insufficient arguments for field insert "
					+ uri);
		}
		
		return doInsert(uri, values, RapidSmsDataDefs.Field.TABLE, RapidSmsDataDefs.Field.NAME);		
	}

	private Uri insertFieldType(Uri uri, ContentValues values) {
		if (values.containsKey(RapidSmsDataDefs.FieldType._ID) == false
				|| values.containsKey(RapidSmsDataDefs.FieldType.NAME) == false
				|| values.containsKey(RapidSmsDataDefs.FieldType.REGEX) == false
				|| values.containsKey(RapidSmsDataDefs.FieldType.DATATYPE) == false) {

			throw new SQLException(
					"Insufficient arguments for fieldtype insert " + uri);
		}
		return doInsert(uri, values, RapidSmsDataDefs.FieldType.TABLE,RapidSmsDataDefs.FieldType.NAME);		
	}
	
	public void ClearFormDataDebug() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor formsCursor = db.query("rapidandroid_form",
				new String[] { "prefix" }, null, null, null, null, null);
		// ("select prefix from rapidandroid_form");

		// iterate and blow away
		formsCursor.moveToFirst();
		do {
			String prefix = formsCursor.getString(0);
			String dropstatement = "drop table formdata_" + prefix + ";";
			db.execSQL(dropstatement);
		} while (formsCursor.moveToNext());
		formsCursor.close();
	}

	/**
	 * @param uri
	 * @param values
	 */
	private Uri insertMessage(Uri uri, ContentValues values) {
		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (values.containsKey(RapidSmsDataDefs.Message.TIME) == false) {
			values.put(RapidSmsDataDefs.Message.TIME, now);
		}

		if (values.containsKey(RapidSmsDataDefs.Message.MESSAGE) == false) {
			throw new SQLException("No message");
		}

		if (values.containsKey(RapidSmsDataDefs.Message.PHONE) == false) {
			throw new SQLException("No message");
		} else {
			ContentValues monitorValues = new ContentValues();
			monitorValues.put(RapidSmsDataDefs.Monitor.PHONE, values
					.getAsString(RapidSmsDataDefs.Message.PHONE));
			Uri monitorUri = insertMonitor(
					RapidSmsDataDefs.Monitor.CONTENT_URI, monitorValues);
			// ok, so we insert the mMonitorString into the mMonitorString table.
			// get the URI back and assign the foreign key into the values as
			// part of the message insert
			values.put(RapidSmsDataDefs.Message.MONITOR, monitorUri
					.getPathSegments().get(1));
		}

		if (values.containsKey(RapidSmsDataDefs.Message.IS_OUTGOING) == false) {
			throw new SQLException("No direction");
		}

		if (values.containsKey(RapidSmsDataDefs.Message.IS_VIRTUAL) == false) {
			values.put(RapidSmsDataDefs.Message.IS_VIRTUAL, false);
		}

		return doInsert(uri, values, RapidSmsDataDefs.Message.TABLE, RapidSmsDataDefs.Message.MESSAGE);
	}

	/**
	 * @param uri
	 * @param values
	 */
	private Uri insertMonitor(Uri uri, ContentValues values) {
		// Make sure that the fields are all set
		if (values.containsKey(RapidSmsDataDefs.Monitor.PHONE) == false) {
			throw new SQLException("No phone");
		}

		if (values.containsKey(RapidSmsDataDefs.Monitor.ALIAS) == false) {
			values.put(RapidSmsDataDefs.Monitor.ALIAS, values
					.getAsString(RapidSmsDataDefs.Monitor.PHONE));
		}

		if (values.containsKey(RapidSmsDataDefs.Monitor.EMAIL) == false) {
			values.put(RapidSmsDataDefs.Monitor.EMAIL, "");
		}

		if (values.containsKey(RapidSmsDataDefs.Monitor.FIRST_NAME) == false) {
			values.put(RapidSmsDataDefs.Monitor.FIRST_NAME, "");
		}

		if (values.containsKey(RapidSmsDataDefs.Monitor.LAST_NAME) == false) {
			values.put(RapidSmsDataDefs.Monitor.LAST_NAME, "");
		}

		if (values.containsKey(RapidSmsDataDefs.Monitor.INCOMING_MESSAGES) == false) {
			values.put(RapidSmsDataDefs.Monitor.INCOMING_MESSAGES, 0);
		}
		
		
		//Check if mMonitorString exists, if it doesn't insert a new one, else return the old one.
		Cursor exists = query(uri, null, RapidSmsDataDefs.Monitor.PHONE + "='"
				+ values.getAsString(RapidSmsDataDefs.Monitor.PHONE) + "'",
				null, null);		

		if (exists.getCount() == 1) {	
			exists.moveToFirst();
			int existingMonitorId = exists.getInt(0);
			exists.close();
			return ContentUris.withAppendedId(RapidSmsDataDefs.Monitor.CONTENT_URI,existingMonitorId);
		} else {
			exists.close();
		}

		Uri ret = doInsert(uri, values, RapidSmsDataDefs.Monitor.TABLE, RapidSmsDataDefs.Monitor.PHONE);
		MessageTranslator.updateMonitorHash(getContext());
		return ret;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String table;
		String finalWhere = "";
		
		switch (sUriMatcher.match(uri)) {
		case MESSAGE:
			table = RapidSmsDataDefs.Message.TABLE;
			break;

		case MESSAGE_ID:
			table = RapidSmsDataDefs.Message.TABLE;
			finalWhere = RapidSmsDataDefs.Message._ID + "="
					+ uri.getPathSegments().get(1)
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
			break;
		case MONITOR:
			table = RapidSmsDataDefs.Monitor.TABLE;
			break;

		case MONITOR_ID:
			table = RapidSmsDataDefs.Monitor.TABLE;
			finalWhere = RapidSmsDataDefs.Message._ID + "="
					+ uri.getPathSegments().get(1)
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
			break;
		case MONITOR_MESSAGE_ID:
			table = RapidSmsDataDefs.Message.TABLE;
			// qb.appendWhere(RapidSmsDataDefs.Message.MONITOR + "="
			// + uri.getPathSegments().get(1));

			finalWhere = RapidSmsDataDefs.Message.MONITOR + "="
					+ uri.getPathSegments().get(1)
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (finalWhere == "") {
			finalWhere = where;
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		int result = db.delete(table, finalWhere, whereArgs);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case MESSAGE:
			qb.setTables(RapidSmsDataDefs.Message.TABLE);
			break;

		case MESSAGE_ID:
			qb.setTables(RapidSmsDataDefs.Message.TABLE);
			qb.appendWhere(RapidSmsDataDefs.Message._ID + "="
					+ uri.getPathSegments().get(1));
			break;
		case MONITOR:
			qb.setTables(RapidSmsDataDefs.Monitor.TABLE);
			break;

		case MONITOR_ID:
			qb.setTables(RapidSmsDataDefs.Monitor.TABLE);
			qb.appendWhere(RapidSmsDataDefs.Monitor._ID + "="
					+ uri.getPathSegments().get(1));
			break;
		case MONITOR_MESSAGE_ID:
			qb.setTables(RapidSmsDataDefs.Message.TABLE);
			qb.appendWhere(RapidSmsDataDefs.Message.MONITOR + "="
					+ uri.getPathSegments().get(1));
			break;
		case FORM:
			qb.setTables(RapidSmsDataDefs.Form.TABLE);
			break;
		case FORM_ID:
			qb.setTables(RapidSmsDataDefs.Form.TABLE);
			qb.appendWhere(RapidSmsDataDefs.Form._ID + "="
					+ uri.getPathSegments().get(1));
			break;
		case FIELD:
			qb.setTables(RapidSmsDataDefs.Field.TABLE);
			break;
		case FIELD_ID:
			qb.setTables(RapidSmsDataDefs.Field.TABLE);
			qb.appendWhere(RapidSmsDataDefs.Field._ID + "="
					+ uri.getPathSegments().get(1));
			break;
		case FIELDTYPE:
			qb.setTables(RapidSmsDataDefs.FieldType.TABLE);
			break;
		case FIELDTYPE_ID:
			qb.setTables(RapidSmsDataDefs.FieldType.TABLE);
			qb.appendWhere(RapidSmsDataDefs.FieldType._ID + "="
					+ uri.getPathSegments().get(1));
			break;
			case FORMDATA_ID:
				// todo: need to set the table to the FieldData + form_prefix
				// this is possible via querying hte forms to get the
				// formname/prefix from the form table definition
				// and appending that to do the qb.setTables
				String formid = uri.getPathSegments().get(1);
				Form f = ModelTranslator.getFormById(Integer.valueOf(formid).intValue());
				qb.setTables(RapidSmsDataDefs.FormData.TABLE_PREFIX + f.getPrefix());
				break;
			//throw new IllegalArgumentException(uri	+ " query handler not implemented.");

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy = sortOrder;

		// if (TextUtils.isEmpty(sortOrder)) {
		// orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
		// } else {
		// orderBy = sortOrder;
		// }

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);
		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		throw new IllegalArgumentException("Update not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite
	 * .SQLiteDatabase)
	 */
	@Override
	public boolean onCreate() {
		mOpenHelper = new SmsDbHelper(getContext());
		return true;
	}

}
