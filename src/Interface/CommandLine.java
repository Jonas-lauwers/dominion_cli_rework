package Interface;

import java.util.*;
import Engine.Deck;
import Engine.Player;
import Engine.GameEngine;
import Database_connection.*;
import Card.Card;

import java.io.IOException;

public class CommandLine {

	   private static GameEngine gameEngine;
    private static Scanner input;
    private static SaveGameConnection saveGameConnection;

    public static void main(String[] args) {

        gameEngine = new GameEngine();
        saveGameConnection = new SaveGameConnection();
        input = new Scanner(System.in);
        showStartScreen();
    }

    private static int getUserInput(String printText, int minimum, int maximum) {
        System.out.print(printText);

        while (!input.hasNextInt()) {
            System.out.print("Wrong input, please enter a number between " + minimum + " and " + maximum + ": ");
            input.nextLine();
        }

        int userInput = input.nextInt();
        while (userInput < minimum || maximum < userInput) {
            System.out.print("Wrong input, please enter a number between " + minimum + " and " + maximum + ": ");
            try {
                userInput = input.nextInt();
            } catch (InputMismatchException e) {
                System.out.print("Wrong input, please enter a number between " + minimum + " and " + maximum + ": ");
                input.nextLine();
                userInput = getUserInput("", minimum, maximum);
            }
        }
        return userInput;
    }

    private static String getUserInput(String printText) {
        System.out.print(printText);
        String userInput = input.next();
        while (userInput.isEmpty()) { //Scanner vraagt om een of andere reden niet naar input voor te checken op empty...?
            System.out.print("Nothing entered. Please enter something: ");
            userInput = input.nextLine();
        }
        return userInput;
    }

    //TODO look for other way to check for input verification or make it more logic deck input like with numbers
    private static String getUserInputDeck() {
        @SuppressWarnings("serial")
        ArrayList<String> options = new ArrayList<String>() {
            {
                add("Kingdom");
                add("Treasure");
                add("Victory");
                add("Curse");
                add("Cancel");
            }
        };
        System.out.print("Enter the name of the deck you want to buy from:");
        String userInput = input.next();
        while (userInput.isEmpty() || !options.contains(userInput)) {
            System.out.print("Not an available option give another deck name: ");
            userInput = input.next();

        }
        return userInput;
    }

    private static void gameSetup() {
        boolean playersAdded = false;
        while (!playersAdded) {
            showAddPlayerMenu(gameEngine.getNumberOfPlayers() == gameEngine.getMaxNumberOfPlayers());
            int choice = getUserInput("Choose option: ", 1, 4);
            switch (choice) {
                case 1:
                    addPlayer();
                    break;
                case 2:
                    showStartScreen();
                    break;
                case 3:
                    playersAdded = true;
                    setupCustomDeck();
                    break;
                case 4:
                    playersAdded = true;
                    boolean existingDeck = false;
                    showAllSets();
                    String deckName = getUserInput("Give the premade deck's name");
                    while (!existingDeck) {
                        try {
                            setupPremadeDeck(deckName);
                            existingDeck = true;
                        } catch (IllegalArgumentException e) {
                            deckName = getUserInput("This name is invalid. Try another name.");
                        }
                    }
            }
        }
    }

    private static void addPlayer() {
        String playerName = getUserInput("Enter name for player: ");
        boolean playerAdded = false;
        while (!playerAdded) {
            try {
                gameEngine.addPlayer(playerName);
                playerAdded = true;
            } catch (IllegalArgumentException e) {
                playerName = getUserInput(e.getMessage() + " Enter an other name: ");
            } catch (IllegalStateException e) {
                System.out.println(e.getMessage());
                break;
            }
        }
    }

    private static void setupCustomDeck() {

        gameEngine.setExpansions("Dominion");
        System.out.println(gameEngine.getChoosableKingdomCards());

        //TODO looks like a big crap ... find another way to do this.
        int[] kingdomCardsIndex = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        System.out.println("Enter 10 kingdom cards you want to use:");
        for (int i = 0; i < kingdomCardsIndex.length; i++) {
            int cardIndex = (getUserInput(i + 1 + ") ", 1, gameEngine.getNumberOfChoosableKingdomCards())) - 1;
            boolean added = false;
            while (!added) {
                boolean exists = false;
                for (int index : kingdomCardsIndex) {
                    if (index == cardIndex) {
                        exists = true;
                    }
                }
                if (!exists) {
                    kingdomCardsIndex[i] = cardIndex;
                    added = true;
                } else {
                    System.out.println("Card " + (cardIndex + 1) + " is already chosen. Choose another one.");
                    cardIndex = (getUserInput(i + 1 + ") ", 1, gameEngine.getNumberOfChoosableKingdomCards())) - 1;
                }
            }
        }
        gameEngine.setPlayableKingdomCards(kingdomCardsIndex);
        gameEngine.startGame();
        playGame();
    }

    private static void setupPremadeDeck(String deckName) {
        gameEngine.setExpansions("Dominion");
        gameEngine.usePresetDeck(deckName);
        gameEngine.startGame();
        playGame();
    }

    private static void loadMenu() {

        showLoadMenu();
        int choice = getUserInput("Choose option: ", 1, 4);
        switch (choice) {
            case 1:
                showSavedGames();
                break;
            case 2:
                loadGame();
                break;
            case 3:
                deleteSavedGame();
                break;
            case 4:
                showStartScreen();
        }
    }

    private static void loadGame() {
        String saveName = getUserInput("Enter your saved game name: ");
        boolean loaded = false;
        while (!loaded) {
            try {
                gameEngine = saveGameConnection.loadGame(saveName);
                loaded = true;
            } catch (Exception e) {
                saveName = getUserInput("The save name was incorrect. Try again: ");
            }
        }
        playGame();

    }

    private static void deleteSavedGame() {
        String saveName = getUserInput("Enter your saved game name:");
        boolean deleted = false;
        while (!deleted) {
            try {
                saveGameConnection.deleteSave(saveName);
                deleted = true;
                System.out.println(saveName + " was deleted!");
                loadMenu();
            } catch (Exception e) {
                saveName = getUserInput("The save name was incorrect. Try again: ");
            }
        }
    }

    private static void makeSet() {
        String setName = getUserInput("Enter your new set's name:");
        ArrayList<String> possibleCards = new cardConnection().getKingdomCards(new String[]{"dominion"});
        ArrayList<String> chosenCards = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            boolean existingCard = false;
            while (!existingCard) {
                String card = getUserInput("Enter the name of card " + i + ": ");
                if (chosenCards.contains(card)) {
                    System.out.println("Card was already selected!");
                } else if (possibleCards.contains(card)) {
                    chosenCards.add(card);
                    existingCard = true;
                } else {
                    System.out.println("Card does not exist!");
                }
            }
        }
        try {
            new PremadeSetsConnection().setKingdomSet(setName, chosenCards);
            System.out.println("Set was successfully created!");
            setMenu();
        } catch (Exception e) {
            System.out.println("Something went wrong!");
        }

    }

    private static void deleteSet() {
        String setName = getUserInput("Enter your premade set name:");
        boolean deleted = false;
        while (!deleted) {
            try {
                new PremadeSetsConnection().deleteSet(setName);
                deleted = true;
                System.out.println(setName + " was deleted!");
                setMenu();
            } catch (Exception e) {
                setName = getUserInput("The set name was incorrect. Try again: ");
            }
        }
    }

    //TODO update playgame and show functions to improve working and look.
    private static void playGame() {
        while (!gameEngine.checkGameEnd()) {
            showStatus(gameEngine.getCurrentPlayer());
            int choice;
            // added check on buys of player because if you play all your coins and actions, wich in the end give you more coins you can't play them anymore.
            if (gameEngine.getCurrentPlayer().getDeck("Hand").isEmpty() || gameEngine.getPhase().equals("Buy")) {
                if (gameEngine.getCurrentPlayer().getBuys() == 0) {
                    showPlayMenu(true, false);
                    choice = getUserInput("Please make a choice:", 3, 4);
                } else {
                    showPlayMenu(true, true);
                    choice = getUserInput("Please make a choice:", 2, 4);
                }
            } else {
                showPlayMenu(false, true);
                choice = getUserInput("Please make a choice:", 1, 4);
            }
            int cardNumber;
            switch (choice) {
                case 1:
                    cardNumber = getUserInput("Enter card number or 0 to cancel: ", 0, gameEngine.getCurrentPlayer().getDeck("Hand").size());
                    if (cardNumber > 0) {
                        Card card = gameEngine.getCurrentPlayer().getDeck("Hand").getCard(cardNumber - 1);
                        playCard(cardNumber - 1, card);
                    }
                    break;
                case 2:
                    showBuyMenu();
                    String deckName = getUserInputDeck();
                    if (deckName.equals("Cancel")) {
                        break;
                    }
                    if (deckName.toLowerCase().equals("curse")) {
                        gameEngine.buyCard(gameEngine.getStack(deckName).getCards()[0],deckName);
                        break;
                    }
                    Card[] cards = gameEngine.getStack(deckName).getCards();
                    cardNumber = getUserInput("Enter card number: ", 1, cards.length);
                    gameEngine.buyCard(cards[cardNumber - 1], deckName);
                    break;
                case 3:
                    gameEngine.endTurn();
                    break;
                case 4:
                    String saveName = getUserInput("Please enter a name for your save file. You can use this name to load your game. Using an existing name wil overwrite the save file.");
                    boolean gameSaved = false;
                    while (!gameSaved) {
                        try {
                            saveGameConnection.setGameEngine(gameEngine);
                            saveGameConnection.saveGame(saveName);
                            gameSaved = true;
                        } catch (Exception e) {
                            saveName = getUserInput("This name is invalid. Try another name.");
                        }
                    }
            }
        }
        gameEngine.endGame();
        showEndGame();
    }

    private static void playCard(int cardNumber, Card card) {
        if (gameEngine.playCard(card)) {
            List<Player> playerList = gameEngine.getPlayers();
            Player player = gameEngine.getCurrentPlayer();
            int userInput;
            String deckName;
            Card[] cards;
            switch (card.getName()) {
                case "Cellar":
                    showPlayerStatus(player);
                    int discardCards = 0;
                    userInput = getUserInput("Enter a cardnumber you want to discard or 0 to continue: ", 0, player.getDeck("Hand").size());
                    while (userInput != 0) {
                        discardCards++;
                        gameEngine.discardFromHand(player, userInput - 1);
                        showPlayerStatus(player);
                        userInput = getUserInput("Enter a cardnumber you want to discard or 0 to continue: ", 0, player.getDeck("Hand").size());
                    }
                    gameEngine.drawCardsFromPlayerDeck(player, discardCards);
                    break;
                case "Chapel":
                    showPlayerStatus(player);
                    int maxTrashedCards = 4;
                    userInput = getUserInput("Enter a cardnumber you want to trash or 0 to continue: ", 0, player.getDeck("Hand").size());
                    while (userInput != 0 && maxTrashedCards > 0) {
                        maxTrashedCards--;
                        gameEngine.trashFromHand(player, userInput - 1);
                        showPlayerStatus(player);
                        userInput = getUserInput("Enter a cardnumber you want to trash or 0 to continue: ", 0, player.getDeck("Hand").size());
                    }
                    break;
                case "Chancellor":
                    userInput = getUserInput("Press 1 to discard your current deck 2 to continue: ", 1, 2);
                    if (userInput == 1) {
                        gameEngine.discardDeck(player);
                    }
                    break;
                case "Workshop":
                    showBuyMenu();
                    deckName = getUserInputDeck();
                    if (deckName.equals("Cancel")) {
                        break;
                    }
                    if (deckName.equals("Curse")) {
                        gameEngine.buyCard(gameEngine.getStack(deckName).getCards()[0], deckName);
                        break;
                    }
                    cards = gameEngine.getStack(deckName).getCards();
                    cardNumber = getUserInput("Enter card number(Max cost 4): ", 1, cards.length) - 1;
                    gameEngine.getCardOfValue(deckName, cards[cardNumber], 4);
                    break;
                case "Feast":
                    gameEngine.trashPlayedCard();
                    showBuyMenu();
                    deckName = getUserInputDeck();
                    if (deckName.equals("Cancel")) {
                        break;
                    }
                    if (deckName.equals("Curse")) {
                        gameEngine.buyCard(gameEngine.getStack(deckName).getCards()[0], deckName);
                        break;
                    }
                    cards = gameEngine.getStack(deckName).getCards();
                    cardNumber = getUserInput("Enter card number(Max cost 5): ", 1, cards.length) - 1;
                    gameEngine.getCardOfValue(deckName, cards[cardNumber], 5);
                    break;
                case "Moneylender":
                    userInput = getUserInput("Press 1 to do the action, press 2 to continue without: ", 1, 2);
                    if (userInput == 1) {
                        boolean copperSelected = false;
                        while (!copperSelected) {
                            showPlayerStatus(player);
                            userInput = getUserInput("Select a copper, or press 0 to cancel: ", 0, player.getDeck("Hand").size());
                            if (userInput == 0) {
                                copperSelected = true;
                            } else if (gameEngine.checkSpecificCard(player.getDeck("Hand").getCard(userInput - 1).getName(), "copper")) {
                                gameEngine.trashFromHand(player, userInput - 1);
                                player.addCoins(3);
                                copperSelected = true;
                            }
                        }
                    }
                    break;
                case "Remodel":
                    showPlayerStatus(player);
                    userInput = getUserInput("Enter cardnumber of the card you wish to trash: ", 1, player.getDeck("Hand").size()) - 1;
                    int maxValue = gameEngine.getSelectedCard(userInput).getCost() + 2;
                    gameEngine.trashFromHand(player, userInput);
                    showBuyMenu();
                    deckName = getUserInputDeck();
                    if (deckName.equals("Cancel")) {
                        break;
                    }
                    if (deckName.equals("Curse")) {
                        gameEngine.buyCard(gameEngine.getStack(deckName).getCards()[0], deckName);
                        break;
                    }
                    cards = gameEngine.getStack(deckName).getCards();
                    cardNumber = getUserInput("Enter card number(Max value " + maxValue + "): ", 1, cards.length) - 1;
                    gameEngine.getCardOfValue(deckName, cards[cardNumber], maxValue);
                    break;
                //TODO make real fix .... if card gets played the index of the card gets changed ... and also when the playing card plays cards thats why it fucks :D
                case "Throne Room":
                    showPlayerStatus(player);
                    int userInputThroneRoom = getUserInput("Choose action card to play twice: ", 1, player.getDeck("Hand").size()) - 1;
                    card = player.getDeck("Hand").getCard(userInputThroneRoom);
                    if (card.isAction()) {
                        playCard(userInputThroneRoom, card);
                        playCard(userInputThroneRoom, card);
                        player.getDeck("Hand").moveCardToDeck(userInputThroneRoom, player.getDeck("Table"));
                    }
                    break;
                case "Council Room":
                    for (int i = 0; i < playerList.size(); i++) {
                        if (player != playerList.get(i)) {
                            gameEngine.drawCardsFromPlayerDeck(playerList.get(i), 1);
                        }
                    }
                    break;
                case "Library":
                    Deck aside = new Deck();
                    while (player.getDeck("Hand").size() < 7) {
                        card = player.getDeck("Deck").pop();
                        if (card == null) {
                            gameEngine.discardDeck(player);
                            card = player.getDeck("Deck").pop();
                        }
                        if (card.isAction()) {
                            userInput = getUserInput("Press 1 to put " + card.toString() + " aside, press 2 to add to your hand: ", 1, 2);
                            if (userInput == 1) {
                                aside.add(card);
                            } else {
                                player.getDeck("Hand").add(card);
                            }
                        } else {
                            player.getDeck("Hand").add(card);
                        }
                    }
                    aside.moveDeckTo(player.getDeck("Discard"));
                    break;
                case "Mine":
                    showPlayerStatus(player);
                    userInput = getUserInput("Give cardnumber of treasure card you wish to trash: ", 1, player.getDeck("Hand").size()) - 1;
                    if (gameEngine.checkSpecificCard(gameEngine.getSelectedCard(userInput).getType(), "Treasure")) {
                        int maxValueMine = gameEngine.getSelectedCard(userInput).getCost() + 3;
                        gameEngine.trashFromHand(player, userInput);
                        showBuyMenu();
                        deckName = getUserInputDeck();
                        if (deckName.equals("Cancel")) {
                            break;
                        }
                        if (deckName.equals("Curse")) {
                        gameEngine.buyCard(gameEngine.getStack(deckName).getCards()[0], deckName);
                            break;
                        }
                        cards = gameEngine.getStack(deckName).getCards();
                        userInput = getUserInput("Enter card number(Max value " + maxValueMine + "): ", 1, cards.length) - 1;
                        gameEngine.getCardOfValue(deckName, cards[userInput], maxValueMine);
                    }
                    break;
                case "Adventurer":
                    ArrayList<Card> temp = new ArrayList<>();
                    int treasureCards = 0;
                    while (treasureCards < 2) {
                        card = player.getDeck("Deck").pop();
                        if (card == null) {
                            gameEngine.discardDeck(player);
                            card = player.getDeck("Deck").pop();
                        }
                        if (card.getType().equals("Treasure")) {
                            player.getDeck("Hand").add(card);
                            treasureCards++;
                        } else {
                            temp.add(card);
                        }
                    }
                    for (Card c : temp) {
                        player.getDeck("Discard").add(c);
                    }
                    break;
                case "Bureaucrat":
                    cards = gameEngine.getStack("treasure").getCards();
                    card = cards[0];
                    gameEngine.drawCardFromTable("treasure", card, player, "Deck");
                    for (int i = 0; i < playerList.size(); i++) {
                        if (playerList.get(i) != player) {
                            if (willGetAttacked(playerList.get(i))) {
                                if (playerList.get(i).getDeck("Hand").hasVictoryCards()) {
                                    clearScreen();
                                    showPlayerStatus(playerList.get(i));
                                    userInput = getUserInput("Choose victory card you want to show: ", 1, playerList.get(i).getDeck("Hand").size());
                                    System.out.println(playerList.get(i).getName() + " is showing:\n" + playerList.get(i).getDeck("Hand").getCard(userInput).toString());
                                    getUserInput("Press 1 to continue", 1, 1);
                                } else {
                                    clearScreen();
                                    System.out.println(playerList.get(i).getName() + " is shows his hand:\n" + playerList.get(i).getDeck("Hand").toString());
                                    getUserInput("Press 1 to continue", 1, 1);
                                }
                            }
                        }
                    }
                    break;
                case "Militia":
                    for (int i = 0; i < playerList.size(); i++) {
                        if (playerList.get(i) != player) {
                            if (willGetAttacked(playerList.get(i))) {
                                while (playerList.get(i).getDeck("Hand").size() > 3) {
                                    clearScreen();
                                    showPlayerStatus(playerList.get(i));
                                    System.out.println(playerList.get(i).getName() + ":");
                                    userInput = getUserInput("Choose card do discard until you only have 3 left in your hand: ", 1, playerList.get(i).getDeck("Hand").size()) - 1;
                                    gameEngine.discardFromHand(playerList.get(i), userInput);
                                }
                            }
                        }
                    }
                    break;
                case "Spy":
                    clearScreen();
                    for (int i = 0; i < playerList.size(); i++) {
                        if (willGetAttacked(playerList.get(i))) {
                            card = playerList.get(i).getDeck("Deck").pop();
                            userInput = getUserInput(player.getName() + " enter 1 to put " + playerList.get(i).getName() + "'s card " + card.getName() + " back on his deck, enter 2 to discard it: ", 1, 2);
                            if (userInput == 1) {
                                playerList.get(i).getDeck("Deck").add(0, card);
                            } else {
                                playerList.get(i).getDeck("Discard").add(card);
                            }
                        }
                    }
                    break;
                case "Thief":
                    clearScreen();
                    ArrayList<Card> trashCards = new ArrayList<>();
                    for (int i = 0; i < playerList.size(); i++) {
                        if (player != playerList.get(i)) {
                            if (willGetAttacked(playerList.get(i))) {
                                ArrayList<Card> tempList = new ArrayList<>();
                                Player playerOne = playerList.get(i);
                                for (int x = 0; x < 2; x++) {
                                    card = playerOne.getDeck("Deck").pop();
                                    tempList.add(card);
                                }
                                for (Card c : tempList) {
                                    if (c.getType().equals("Treasure")) {
                                        System.out.println(playerOne.getName() + " shows " + c.toString());
                                        userInput = getUserInput("Press 1 to trash/steal card or press 2 to discard: ", 1, 2);
                                        if (userInput == 1) {
                                            trashCards.add(c);
                                        } else {
                                            playerOne.getDeck("Discard").add(c);
                                        }
                                    } else {
                                        playerOne.getDeck("Discard").add(c);
                                    }

                                }
                            }
                        }
                    }
                    for (Card c : trashCards) {
                        userInput = getUserInput("Enter 1 to steal " + c.getName() + " or press 2 to discard it: ", 1, 2);
                        if (userInput == 1) {
                            player.getDeck("Discard").add(c);
                        } else {
                            player.getDeck("Trash").add(c);
                        }
                    }

                    break;
                case "Witch":
                    for (int i = 0; i < playerList.size(); i++) {
                        if (player != playerList.get(i)) {
                            if (willGetAttacked(playerList.get(i))) {
                                card = gameEngine.getStack("curse").getCards()[0];
                                gameEngine.drawCardFromTable("curse", card, playerList.get(i),"discard");
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean willGetAttacked(Player player) {
        if (player.getDeck("Hand").hasReactionCards()) {
            int userInput = getUserInput(player.getName() + " press 1 to counter attack card . press 2 to do nothing: ", 1, 2);
            if (userInput == 1) {
                return false;
            }
        }
        return true;
    }

    private static void showStartScreen() {
        System.out.println("Welcome to Dominion");
        System.out.println("-------------------");
        System.out.println("1) Start a new game");
        System.out.println("2) Load game menu");
        System.out.println("3) Premade sets options");
        System.out.println("4) Exit");
        int choice = getUserInput("Please make a choice (1-4): ", 1, 4);
        switch (choice) {
            case 1:
                gameSetup();
                break;
            case 2:
                loadMenu();
                break;
            case 3:
                setMenu();
                break;
            case 4:
                System.exit(0);
                break;
        }

    }

    private static void showAddPlayerMenu(boolean maxPlayersReached) {
        if (!maxPlayersReached) {
            System.out.println("1) Add player");
        }
        System.out.println("2) Cancel");
        System.out.println("3) Play with custom deck");
        System.out.println("4) Play with premade deck");
    }

    private static void setMenu() {
        showSetMenu();
        int choice = getUserInput("Choose option: ", 1, 4);
        switch (choice) {
            case 1:
                showAllSets();
                setMenu();
                break;
            case 2:
                makeSet();
                break;
            case 3:
                deleteSet();
                break;
            case 4:
                showStartScreen();
        }
    }

    private static void showSetMenu() {
        System.out.println("1) Show all existing premade sets");
        System.out.println("2) Make a new set");
        System.out.println("3) Delete an existing set");
        System.out.println("4) Back");
    }

    private static void showAllSets() {
        ArrayList<String> allSets = new PremadeSetsConnection().getAllSetNames();
        int i = 1;
        for (String setName : allSets) {
            System.out.println(i++ + ": " + setName);
        }
        //setMenu();
    }

    private static void showLoadMenu() {
        System.out.println("1) Show all saved games");
        System.out.println("2) Load a game");
        System.out.println("3) Delete a saved game");
        System.out.println("4) Back");
    }

    private static void showSavedGames() {
        ArrayList<String> savedGames = new SaveGameConnection().getAllSaveNames();
        int i = 1;
        for (String gameName : savedGames) {
            System.out.println(i++ + ": " + gameName);
        }
        loadMenu();
    }

    private static void showPlayMenu(boolean emptyHand, boolean buysLeft) {
        if (!emptyHand) {
            System.out.println("1) use card");
        }
        if (buysLeft) {
            System.out.println("2) buy card");
        }
        System.out.println("3) end turn");
        System.out.println("4) save game");
    }

    private static void showBuyMenu() {
        System.out.println("1) Treasure");
        System.out.println("2) Kingdom");
        System.out.println("3) Victory");
        System.out.println("4) Curse");
        System.out.println("5) Cancel");
    }

    private static void showEndGame() {
        clearScreen();
        System.out.println("Game has ended!!!!");
        System.out.println("Endscore:");
        List<Player> player = gameEngine.getPlayers();
        for (Player p : player) {
            System.out.println("\t" + p.getName() + " Points: " + p.getScore());
        }
    }

    private static void showStatus(Player player) {
        clearScreen();
        System.out.printf("\n%s\n", gameEngine.getTableStatus());
        System.out.printf("\n%s\n", gameEngine.getPlayerStatus(player));
    }

    private static void showPlayerStatus(Player player) {
        clearScreen();
        System.out.printf("\n%s\n", gameEngine.getPlayerStatus(player));
    }

    private static void clearScreen() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}
