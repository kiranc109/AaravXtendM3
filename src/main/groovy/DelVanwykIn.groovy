/****************************************************************************************
 Extension Name: EXT003MI/DelVanwykIn
 Type: ExtendM3Transaction
 Script Author: Abhijit.Patil@harman.com
 Date: 2026-03-24
 Description:
 * Deleting records from EXTVWI table

 Revision History:
 Name                         Date         Version   Description of Changes
 Abhijit.Patil@harman.com     2026-03-24   1.0       Initial version - Delete record from EXTVWI table
*****************************************************************************************/

public class DelVanwykIn extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;
  private final MICallerAPI miCaller;

  private String inMfno, inType, inIngr;
  private int inCono, inDlix, inPlsx, inSqor;
  private boolean validInput = true;

  public DelVanwykIn(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller) {
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

    validateInput();
    if (validInput) {
      deleteRecord();
    }
  }

  /**
   * Validate input fields before deleting a record
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
  }

  /**
   * Delete records from EXTVWI table
   * @params
   * @return
   */
  public void deleteRecord() {
    DBAction query = database.table("EXTVWI").index("00").build();
    DBContainer container = query.getContainer();

    container.set("EXCONO", inCono);
    container.set("EXTYPE", inType);
    container.set("EXMFNO", inMfno);
    container.set("EXDLIX", inDlix);
    container.set("EXPLSX", inPlsx);
    container.set("EXSQOR", inSqor);
    container.set("EXINGR", inIngr);

    boolean recordFound = query.readLock(container, { LockedResult lockedResult -> lockedResult.delete(); });
    if (!recordFound) {
      mi.error("Record Doesn't Exist.");
      return;
    }
  }
}
