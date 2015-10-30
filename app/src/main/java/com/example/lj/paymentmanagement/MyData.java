package com.example.lj.paymentmanagement;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.BoringLayout;
import android.widget.ArrayAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by lj on 15/9/30.
 */

public class MyData extends SQLiteOpenHelper{

    public static MyData myData;
    public static ArrayList<MyAccount> allMyAccounts = new ArrayList<MyAccount>();

    public static ArrayList<String> displayAccountList = new ArrayList<String>();
    public static ArrayAdapter<String> accountListAdapter;

    public static ArrayList<MyPaymentItem> allMyPaymentItems = new ArrayList<MyPaymentItem>();
    public static ArrayList<String> displayPaymentList = new ArrayList<String>();
    public static ArrayAdapter<String> paymentListAdapter;

    public static boolean confirmedToDelete = false;

    public static Integer selectAccountIndex = -1;
    public static Integer selectPaymentItemIndex = -1;

    public static MyAccount selectedAccount;
    public static MyAccount editAccount;
    public static MyAccount newAccount;
    public static MyPaymentItem selectedPaymentItem;
    public static MyPaymentItem editPaymentItem;
    public static MyPaymentItem newPaymentItem;

    //database information
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "myAccountsAndPayments.db";

    //column names for the account table
    private static final String TABLE_ACCOUNTS = "myAccounts";
    private static final String COLUMN_ACCOUNT_ID = "_id";
    private static final String COLUMN_ACCOUNT_NAME = "accountName";
    private static final String COLUMN_ACCOUNT_BANK = "accountBank";
    private static final String COLUMN_ACCOUNT_DUEDAY = "accountDueDay";
    private static final String COLUMN_ACCOUNT_STA_BALANCE = "accountStaBalance";
    private static final String COLUMN_ACCOUNT_STA_DAY = "accountStaDay";
    private static final String COLUMN_ACCOUNT_PUR_APR = "accountPurchaseAPR";

    //column names for the payment table
    private static final String TABLE_PAYMENTS = "myPayments";
    private static final String COLUMN_PAYMENT_ID = "_id";
    private static final String COLUMN_PAY_ACCOUNT = "payAccount";
    private static final String COLUMN_PAID_ACCOUNT = "paidAccount";
    private static final String COLUMN_PAY_AMOUNT = "payAmount";
    private static final String COLUMN_PAY_DATE = "payDate";
    private static final String COLUMN_PAY_TOTAL_THIS_MONTH = "payTotalThisMonth";
    ///////////////////////////////////////////////////

    public MyData(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
        updateAllMyPaymentItemsFromDatabase();
        updateAllAccountsFromDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //create account table;
        String query = "CREATE TABLE " + TABLE_ACCOUNTS + "(" +
                COLUMN_ACCOUNT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ACCOUNT_NAME + " TEXT, " +
                COLUMN_ACCOUNT_BANK + " TEXT, " +
                COLUMN_ACCOUNT_DUEDAY + " INTEGER, " +
                COLUMN_ACCOUNT_STA_DAY + " INTEGER, " +
                COLUMN_ACCOUNT_STA_BALANCE + " DOUBLE, " +
                COLUMN_ACCOUNT_PUR_APR + " DOUBLE " +
                ");";
        db.execSQL(query);

        //create payment table;
        query = "CREATE TABLE " + TABLE_PAYMENTS + "(" +
                COLUMN_ACCOUNT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PAY_ACCOUNT + " TEXT, " +
                COLUMN_PAID_ACCOUNT + " TEXT, " +
                COLUMN_PAY_AMOUNT + " DOUBLE, " +
                COLUMN_PAY_DATE + " TEXT, " +
                COLUMN_PAY_TOTAL_THIS_MONTH + " DOUBLE " +
                ");";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYMENTS);
        onCreate(db);
    }



///////////////////Account operations/////////////////////
///////////////////Account operations/////////////////////

    public void updateAllAccountsFromDatabase(){
        allMyAccounts.clear();
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_ACCOUNTS + ";";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            if (cursor.getString(cursor.getColumnIndex(COLUMN_ACCOUNT_NAME)) != null){
                MyAccount tmp = new MyAccount(
                        cursor.getString(cursor.getColumnIndex(COLUMN_ACCOUNT_NAME)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_ACCOUNT_BANK)),
                        new Integer(cursor.getInt(cursor.getColumnIndex(COLUMN_ACCOUNT_DUEDAY))),
                        new Integer(cursor.getInt(cursor.getColumnIndex(COLUMN_ACCOUNT_STA_DAY))),
                        new Double(cursor.getDouble(cursor.getColumnIndex(COLUMN_ACCOUNT_STA_BALANCE))),
                        new Double(cursor.getDouble(cursor.getColumnIndex(COLUMN_ACCOUNT_PUR_APR))));
                allMyAccounts.add(tmp);
            }
            cursor.moveToNext();
        }
        db.close();
        updateAllAccountsTotalPaidCurrentBillingCycle();
        updateAllAccountsPaidTimes();
    }

    public void updateAllAccountsTotalPaidCurrentBillingCycle(){
        for(int i = 0; i < allMyAccounts.size(); i++){
            MyAccount account = allMyAccounts.get(i);
            account.totalPayThisMonth =
                    getAccountAlreadyPaidAmountCurrentBillingCycle(account);
            account.updateToPayBalance();
            allMyAccounts.set(i, account);
        }
    }

    public void updateAccountListView(){
//        updateAllAccountsFromDatabase();
        displayAccountList.clear();
        Double totalNeedToPay = 0.0;
        Double totalInterest = 0.0;
        for(MyAccount account: allMyAccounts){
            displayAccountList.add(account.toString());
            if(account.toPayBalance > 0.0){
                totalNeedToPay += account.toPayBalance;
            }
            totalInterest += account.toPayInterest;
        }
        displayAccountList.add("Total " +
                new Integer(allMyAccounts.size()).toString() +
                " Accounts\nTotal Balance To Pay: $" +
                MyLib.roundTo2DecimalPoints(totalNeedToPay).toString() +
                        "\nTotal Interest: $" +
                MyLib.roundTo2DecimalPoints(totalInterest).toString());
        accountListAdapter.notifyDataSetChanged();
    }

    public void addAccountToDatabase(MyAccount myAccount){
//        allMyAccounts.add(myAccount);
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACCOUNT_NAME, myAccount.accountName);
        values.put(COLUMN_ACCOUNT_BANK, myAccount.bankName);
        values.put(COLUMN_ACCOUNT_DUEDAY, myAccount.dueDay);
        values.put(COLUMN_ACCOUNT_STA_DAY, myAccount.staDay);
        values.put(COLUMN_ACCOUNT_STA_BALANCE, myAccount.statementBalance);
        values.put(COLUMN_ACCOUNT_PUR_APR, myAccount.purchaseAPR);
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_ACCOUNTS, null, values);
        db.close();
        updateAllAccountsFromDatabase();
        updateAccountListView();
    }

    static public boolean ifAccountAlreadyExist(String accountName){
        for(MyAccount account: allMyAccounts){
            if(account.accountName.equalsIgnoreCase(accountName)){
                return true;
            }
        }
        return false;
    }

    private Double getAccountAlreadyPaidAmount(String accountName){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_PAYMENTS +
                " WHERE " + COLUMN_PAY_ACCOUNT + "=\"" + accountName + "\"";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        Double totalPaidAmount = 0.0;
        while(!cursor.isAfterLast()){
            totalPaidAmount += cursor.getDouble(
                    cursor.getColumnIndex(COLUMN_PAY_AMOUNT));
            cursor.moveToNext();
        }
        db.close();
        return new Double(totalPaidAmount);
    }

    private Double getAccountAlreadyPaidAmountCurrentBillingCycle(MyAccount account){
        updateAllMyPaymentItemsFromDatabase();
        Calendar c = Calendar.getInstance();
        int curDay = c.get(Calendar.DAY_OF_MONTH);
        int dueDay = account.dueDay;
        int staDay = account.staDay;
        int staMonth = c.get(Calendar.MONTH)+1;
        int staYear = c.get(Calendar.YEAR);
        Double total = 0.0;
        for(int i = 0; i < allMyPaymentItems.size(); i++){
            if(allMyPaymentItems.get(i).payAccountName.equals(account.accountName)){
                int payDay = Integer.parseInt(allMyPaymentItems.get(i).payDate.split("/")[2]);
                int payMonth = Integer.parseInt(allMyPaymentItems.get(i).payDate.split("/")[1]);
                int payYear = Integer.parseInt(allMyPaymentItems.get(i).payDate.split("/")[0]);
                if((payDay > staDay && payMonth == staMonth && payYear== staYear) ||
                        (payDay <= staDay && payMonth == staMonth && payYear == staYear)){
                    total += allMyPaymentItems.get(i).payAmount;
                }
            }
        }
        return total;
    }

    public void updateAllAccountsPaidTimes(){
        for(int i = 0; i < allMyAccounts.size(); i++){
            MyAccount account = allMyAccounts.get(i);
            int paidTimes = 0;
            for(int j = 0; j < allMyPaymentItems.size(); j++){
                if(allMyPaymentItems.get(j).payAccountName == account.accountName){
                    paidTimes++;
                }
            }
            account.paidTimes = paidTimes;
            allMyAccounts.set(i, account);
        }
    }

    public void deleteSelectedAccount(){
        if(selectedAccount == null || selectedAccount.accountName == ""){
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        String query = "DELETE FROM " + TABLE_ACCOUNTS + " WHERE "
                + COLUMN_ACCOUNT_NAME + "=\"" + selectedAccount.accountName + "\" AND " +
                COLUMN_ACCOUNT_BANK + "=\"" + selectedAccount.bankName + "\";";
        db.execSQL(query);
        query = "DELETE  FROM " + TABLE_PAYMENTS + " WHERE "
                + COLUMN_PAY_ACCOUNT + "=\"" +
                selectedAccount.accountName +"\";";
        db.execSQL(query);
        db.close();

        //reset global variables
        confirmedToDelete = false;
        selectedAccount = null;
        selectAccountIndex = -1;

        updateAllAccountsFromDatabase();
        updateAllMyPaymentItemsFromDatabase();
        updateAccountListView();
//        updatePaymentListView();
    }

    public void changeSelectedAccount(){
        SQLiteDatabase db = this.getWritableDatabase();

        //change payment name if account name changed
        if(!selectedAccount.accountName.
                equalsIgnoreCase(editAccount.accountName)){
            String q = "UPDATE "+TABLE_PAYMENTS+" SET " +
                    COLUMN_PAY_ACCOUNT + "=\"" + editAccount.accountName + "\"" +
                    " WHERE " +
                    COLUMN_PAY_ACCOUNT + "=\"" + selectedAccount.accountName+"\"";
            db.execSQL(q);
        }

        //update the account in the database
        String query = "UPDATE "+TABLE_ACCOUNTS+" SET " +
                COLUMN_ACCOUNT_NAME + "=\"" + editAccount.accountName + "\", "+
                COLUMN_ACCOUNT_BANK + "=\"" + editAccount.bankName + "\", "+
                COLUMN_ACCOUNT_STA_BALANCE + "=" + editAccount.statementBalance.toString() + ", " +
                COLUMN_ACCOUNT_DUEDAY + "=" + editAccount.dueDay.toString() + ", " +
                COLUMN_ACCOUNT_STA_DAY + "=" + editAccount.staDay.toString() + ", " +
                COLUMN_ACCOUNT_PUR_APR + "=" + editAccount.purchaseAPR.toString() +
                " WHERE " +
                COLUMN_ACCOUNT_NAME + "=\"" + selectedAccount.accountName +"\" AND " +
                COLUMN_ACCOUNT_BANK + "=\"" + selectedAccount.bankName + "\"";
        db.execSQL(query);
        db.close();

        //update the account in the allAccountsList
        updateAllAccountsFromDatabase();


        updateAccountListView();
    }


///////////////////Payment operations/////////////////////
///////////////////Payment operations/////////////////////

    public void updateAllMyPaymentItemsFromDatabase(){
        allMyPaymentItems.clear();
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_PAYMENTS + ";";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            if (cursor.getString(cursor.getColumnIndex(COLUMN_PAY_ACCOUNT)) != null) {
                MyPaymentItem tmp = new MyPaymentItem(
                        cursor.getString(cursor.getColumnIndex(COLUMN_PAY_ACCOUNT)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_PAID_ACCOUNT)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_PAY_AMOUNT)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_PAY_DATE))
                );
                allMyPaymentItems.add(tmp);
            }
            cursor.moveToNext();
        }
        db.close();

    }

    public void updatePaymentListView(){
//        updateAllMyPaymentItemsFromDatabase();
        displayPaymentList.clear();
        for(MyPaymentItem paymentItem: allMyPaymentItems){
            displayPaymentList.add(paymentItem.toString());
        }
        displayPaymentList.add("Total payment in this month is: $" +
                getCurrentTotalPayThisMonth().toString());
        paymentListAdapter.notifyDataSetChanged();
    }

    public void addPaymentToDatabase(MyPaymentItem myPayment){
        allMyPaymentItems.add(myPayment);
        ContentValues values = new ContentValues();
        values.put(COLUMN_PAY_ACCOUNT, myPayment.payAccountName);
        values.put(COLUMN_PAID_ACCOUNT, myPayment.paidAccountName);
        values.put(COLUMN_PAY_AMOUNT, myPayment.payAmount);
        values.put(COLUMN_PAY_DATE, myPayment.payDate);
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_PAYMENTS, null, values);
        db.close();

        updateAllAccountsFromDatabase();
        updateAllMyPaymentItemsFromDatabase();

        updatePaymentListView();

    }

    public void deleteSelectedPaymentItem(){
        if(selectedPaymentItem == null || selectedPaymentItem.payAccountName == ""){
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_PAYMENTS + " WHERE "
                + COLUMN_PAY_ACCOUNT + "=\"" +
                selectedPaymentItem.payAccountName + "\" AND " +
                COLUMN_PAY_AMOUNT + "=" +
                selectedPaymentItem.payAmount.toString()
                + " AND " +
                COLUMN_PAY_DATE + "=\"" + selectedPaymentItem.payDate + "\";");
        db.close();

        updateAllMyPaymentItemsFromDatabase();
        updateAllAccountsFromDatabase();

        //reset global variables
        confirmedToDelete = false;
        selectedPaymentItem = null;
        selectPaymentItemIndex = -1;

        //update current view
        updatePaymentListView();
        updateAccountListView();
    }

    public void changeSelectedPaymentItem(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE "+TABLE_PAYMENTS+" SET " +
                COLUMN_PAY_ACCOUNT + "=\"" + editPaymentItem.payAccountName + "\", "+
                COLUMN_PAID_ACCOUNT + "=\"" + editPaymentItem.paidAccountName + "\", "+
                COLUMN_PAY_AMOUNT + "=" + editPaymentItem.payAmount.toString() + ", " +
                COLUMN_PAY_DATE + "=\"" + editPaymentItem.payDate + "\" " +
                " WHERE " +
                COLUMN_PAY_ACCOUNT + "=\"" + selectedPaymentItem.payAccountName+"\" AND " +
                COLUMN_PAID_ACCOUNT + "=\"" + selectedPaymentItem.paidAccountName + "\" AND " +
                COLUMN_PAY_AMOUNT + "=" + selectedPaymentItem.payAmount.toString() + " AND " +
                COLUMN_PAY_DATE + "=\"" + selectedPaymentItem.payDate + "\";";
        db.execSQL(query);
        db.close();

        updateAllMyPaymentItemsFromDatabase();
        updateAllAccountsFromDatabase();

        updatePaymentListView();
    }

    private Double getCurrentTotalPayThisMonth(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_PAYMENTS + ";";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        Double totalBalance = 0.0;
        while(!cursor.isAfterLast()){
            totalBalance += cursor.getDouble(
                    cursor.getColumnIndex(COLUMN_PAY_AMOUNT));
            cursor.moveToNext();
        }
        db.close();
        return MyLib.roundTo2DecimalPoints(totalBalance);
    }

}
