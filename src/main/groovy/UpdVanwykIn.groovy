/****************************************************************************************
 Extension Name: EXT003MI/UpdVanwykIn
 Type: ExtendM3Transaction
 Script Author: Abhijit.Patil@harman.com
 Date: 2026-03-29
 Description:
 * Update records in EXTVWI table

 Revision History:
 Name                         Date         Version   Description of Changes
 Abhijit.Patil@harman.com     2026-03-29   1.0       Initial version - Update records to EXTVWI table
*****************************************************************************************/

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UpdVanwykIn extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;
  private final MICallerAPI miCaller;

  private String inMfno, inType, inIngr, inInnm, inWhsl, inBano, inUnms;
  private int inCono, inDlix, inPlsx, inSqor;
  private double inTrqt, inDsqt, inBqty;
  private boolean validInput = true;

  public UpdVanwykIn(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller) {
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
   * Update record in EXTVWI table
   * @params
   * @return
   */
  public void updateRecord() {
    LocalDateTime now = LocalDateTime.now();

    DBAction query = database.table("EXTVWI").index("00").build();
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

        if (!inInnm.trim().isEmpty()) {
          lockedResult.set("EXINNM", inInnm.trim().equals("?") ? "" : inInnm);
        }
        if (!inBano.trim().isEmpty()) {
          lockedResult.set("EXBANO", inBano.trim().equals("?") ? "" : inBano);
        }
        if (!inWhsl.trim().isEmpty()) {
          lockedResult.set("EXWHSL", inWhsl.trim().equals("?") ? "" : inWhsl);
        }
        if (mi.inData.get("TRQT") != null && !mi.inData.get("TRQT").trim().isEmpty()) {
          lockedResult.set("EXTRQT", inTrqt);
        }
        if (mi.inData.get("DSQT") != null && !mi.inData.get("DSQT").trim().isEmpty()) {
          lockedResult.set("EXDSQT", inDsqt);
        }
        if (!inUnms.trim().isEmpty()) {
          lockedResult.set("EXUNMS", inUnms.trim().equals("?") ? "" : inUnms);
        }
        if (mi.inData.get("BQTY") != null && !mi.inData.get("BQTY").trim().isEmpty()) {
          lockedResult.set("EXBQTY", inBqty);
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
