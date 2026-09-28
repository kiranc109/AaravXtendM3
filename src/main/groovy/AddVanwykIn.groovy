/****************************************************************************************
 Extension Name: EXT003MI/AddVanwykIn
 Type: ExtendM3Transaction
 Script Author: Abhijit.Patil@harman.com
 Date: 2026-03-29
 Description:
 * Adding records to EXTVWI table

 Revision History:
 Name                         Date         Version   Description of Changes
 Abhijit.Patil@harman.com     2026-03-29   1.0       Initial version - Adding records to EXTVWI table
*****************************************************************************************/

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddVanwykIn extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;
  private final MICallerAPI miCaller;

  private String inMfno, inType, inIngr, inInnm, inWhsl, inBano, inUnms;
  private int inCono, inDlix, inPlsx, inSqor;
  private double inTrqt, inDsqt, inBqty;
  private boolean validInput = true;

  public AddVanwykIn(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller) {
    this.mi = mi;
    this.database = database;
    this.program = program;
    this.miCaller = miCaller;
  }

  /**
   * Main function
   * @param
   * @return
   */
  public void main() {
    inCono = (mi.inData.get("CONO") == null || mi.inData.get("CONO").trim().isEmpty()) ? program.LDAZD.CONO as Integer : mi.inData.get("CONO") as Integer;
    inType = (mi.inData.get("TYPE") == null || mi.inData.get("TYPE").trim().isEmpty()) ? "" : mi.inData.get("TYPE");
    inMfno = (mi.inData.get("MFNO") == null || mi.inData.get("MFNO").trim().isEmpty()) ? "" : mi.inData.get("MFNO");
    inDlix = (mi.inData.get("DLIX") == null || mi.inData.get("DLIX").trim().isEmpty()) ? 0 : mi.inData.get("DLIX") as Integer;
    inPlsx = (mi.inData.get("PLSX") == null || mi.inData.get("PLSX").trim().isEmpty()) ? 0 : mi.inData.get("PLSX") as Integer;
    inSqor = (mi.inData.get("SQOR") == null || mi.inData.get("SQOR").trim().isEmpty()) ? 0 : mi.inData.get("SQOR") as Integer;
    inIngr = (mi.inData.get("INGR") == null || mi.inData.get("INGR").trim().isEmpty()) ? "" : mi.inData.get("INGR");
    inInnm = (mi.inData.get("INNM") == null || mi.inData.get("INNM").trim().isEmpty()) ? "" : mi.inData.get("INNM");
    inWhsl = (mi.inData.get("WHSL") == null || mi.inData.get("WHSL").trim().isEmpty()) ? "" : mi.inData.get("WHSL");
    inTrqt = (mi.inData.get("TRQT") == null || mi.inData.get("TRQT").trim().isEmpty()) ? 0 : mi.inData.get("TRQT") as Double;
    inDsqt = (mi.inData.get("DSQT") == null || mi.inData.get("DSQT").trim().isEmpty()) ? 0 : mi.inData.get("DSQT") as Double;
    inBano = (mi.inData.get("BANO") == null || mi.inData.get("BANO").trim().isEmpty()) ? "" : mi.inData.get("BANO");
    inUnms = (mi.inData.get("UNMS") == null || mi.inData.get("UNMS").trim().isEmpty()) ? "" : mi.inData.get("UNMS");
    inBqty = (mi.inData.get("BQTY") == null || mi.inData.get("BQTY").trim().isEmpty()) ? 0 : mi.inData.get("BQTY") as Double;

    validateInput();
    if (validInput) {
      insertRecord();
    }
  }

  /**
   * Validate input fields before inserting a record
   * @params
   * @return
   */
  public void validateInput() {

    // Validate Order No (MFNO) - must not be blank
    if (inMfno == null || inMfno.trim().isEmpty()) {
      mi.error("Order No (MFNO) must not be blank.");
      validInput = false;
      return;
    }

    // Validate Company Number
    Map<String, String> params = ["CONO": inCono.toString().trim()];
    Closure<?> callback = {
      Map<String, String> response ->
        if (response.CONO == null) {
          mi.error("Invalid Company Number " + inCono);
          validInput = false;
          return;
        }
    }
    miCaller.call("MNS095MI", "Get", params, callback);
    if (!validInput) {
      return;
    }

    // Validate Delivery pick list and Order No
    this.miCaller.setListMaxRecords(1);
    params = ["DLIX": inDlix.toString().trim(), "PLSX": inPlsx.toString().trim()];
    callback = {
      Map<String, String> response ->
        if (response.CONO == null) {
          mi.error("Pick list Details does not exist. Delivery:" + inDlix + " Picking List:" + inPlsx);
          validInput = false;
          return;
        } else if (response.RIDN != null && !response.RIDN.equals(inMfno)) {
          mi.error("Order no: " + inMfno + " not connected to Delivery:" + inDlix + " Picking List:" + inPlsx);
          validInput = false;
          return;
        }
    }
    miCaller.call("MWS422MI", "SelPickDetail", params, callback);
    if (!validInput) {
      return;
    }

    // Validate Ingredients
    params = ["FILE": "MITMAS", "CFI1": inIngr.toString().trim()];
    callback = {
      Map<String, String> response ->
        if (response.CFI1 == null) {
          mi.error("Ingredient:" + inIngr + " does not exist.");
          validInput = false;
          return;
        }
    }
    miCaller.call("CRS181MI", "Get", params, callback);
    if (!validInput) {
      return;
    }
  }

  /**
   * Insert records to EXTVWI table
   * @params
   * @return
   */
  public void insertRecord() {
    DBAction query = database.table("EXTVWI").index("00").build();
    DBContainer container = query.getContainer();

    LocalDateTime now = LocalDateTime.now();

    container.set("EXCONO", inCono);
    container.set("EXDIVI", program.LDAZD.DIVI);
    container.set("EXTYPE", inType);
    container.set("EXMFNO", inMfno);
    container.set("EXDLIX", inDlix);
    container.set("EXPLSX", inPlsx);
    container.set("EXSQOR", inSqor);
    container.set("EXINGR", inIngr);
    container.set("EXINNM", inInnm);
    container.set("EXWHSL", inWhsl);
    container.set("EXTRQT", inTrqt);
    container.set("EXBANO", inBano);
    container.set("EXDSQT", inDsqt);
    container.set("EXUNMS", inUnms);
    container.set("EXBQTY", inBqty);
    container.set("EXRGDT", now.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger());
    container.set("EXRGTM", now.format(DateTimeFormatter.ofPattern("HHmmss")).toInteger());
    container.set("EXLMDT", now.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger());
    container.set("EXCHNO", 1);
    container.set("EXCHID", program.getUser());
    query.insert(container, { mi.error("Record Already Exists"); });
  }
}
