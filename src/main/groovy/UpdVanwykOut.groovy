/****************************************************************************************
 Extension Name: EXT002MI/UpdVanwykOut
 Type: ExtendM3Transaction
 Script Author: Abhijit.Patil@harman.com
 Date: 2026-03-29
 Description:
 * Update record in EXTVWO table

 Revision History:
 Name                         Date         Version   Description of Changes
 Abhijit.Patil@harman.com     2026-03-29   1.0       Initial version - Update record from EXTVWO table
*****************************************************************************************/

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UpdVanwykOut extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;
  private final MICallerAPI miCaller;

  private String inMfno, inType, inIngr;
  private int inCono, inDlix, inPlsx, inSqor;
  private double inTrqt, inTqty;
  private boolean validInput = true;

  public UpdVanwykOut(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller) {
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
    inTrqt = (mi.inData.get("TRQT") == null || mi.inData.get("TRQT").trim().isEmpty()) ? 0 : mi.inData.get("TRQT") as Double;
    inTqty = (mi.inData.get("TQTY") == null || mi.inData.get("TQTY").trim().isEmpty()) ? 0 : mi.inData.get("TQTY") as Double;

    validateInput();
    if (validInput) {
      updateRecord();
    }
  }

  /**
   * Validate input fields before updating a record
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
   * Update record in EXTVWO table
   * @params
   * @return
   */
  public void updateRecord() {
    LocalDateTime now = LocalDateTime.now();

    DBAction query = database.table("EXTVWO").index("00").build();
    DBContainer container = query.getContainer();

    container.set("EXCONO", inCono);
    container.set("EXTYPE", inType);
    container.set("EXMFNO", inMfno);
    container.set("EXDLIX", inDlix);
    container.set("EXPLSX", inPlsx);
    container.set("EXSQOR", inSqor);
    container.set("EXINGR", inIngr);

    boolean recordFound = query.readLock(container, {
      LockedResult lockedResult ->
        int changeNo = lockedResult.get("EXCHNO");

        if (mi.inData.get("TRQT") != null && !mi.inData.get("TRQT").trim().isEmpty()) {
          lockedResult.set("EXTRQT", inTrqt);
        }
        if (mi.inData.get("TQTY") != null && !mi.inData.get("TQTY").trim().isEmpty()) {
          lockedResult.set("EXTQTY", inTqty);
        }

        lockedResult.set("EXLMDT", now.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger());
        lockedResult.set("EXCHNO", changeNo + 1);
        lockedResult.set("EXCHID", program.getUser());
        lockedResult.update();
    });

    if (!recordFound) {
      mi.error("Record Doesn't Exist.");
    }
  }
}
